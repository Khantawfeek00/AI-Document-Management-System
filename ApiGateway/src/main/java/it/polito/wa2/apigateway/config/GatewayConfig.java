package it.polito.wa2.apigateway.config;

import it.polito.wa2.apigateway.filter.TokenRelayFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayConfig {

    private final TokenRelayFilter tokenRelayFilter;

    @Value("${backend.file-service-url}")
    private String fileServiceUrl;

    @Value("${backend.user-service-url}")
    private String userServiceUrl;

    public GatewayConfig(TokenRelayFilter tokenRelayFilter) {
        this.tokenRelayFilter = tokenRelayFilter;
    }

    private HandlerFilterFunction<ServerResponse, ServerResponse> pathRewriteFilter(String from, String to) {
        return (request, next) -> {
            String originalPath = request.path();
            String newPath = originalPath.replaceAll(from, to);

            if (!newPath.equals(originalPath)) {
                ServerRequest modifiedRequest = ServerRequest.from(request)
                        .uri(request.uri().resolve(newPath))
                        .build();
                return next.handle(modifiedRequest);
            } else {
                return next.handle(request);
            }
        };
    }

    @Bean
    public RouterFunction<ServerResponse> fileServiceRoute() {
        return route("file-service")
                .route(path("/api/files/**"), http(fileServiceUrl))
                .filter(pathRewriteFilter("^/api/files", "/api/v1/files"))
                .filter(tokenRelayFilter)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> uploadServiceRoute() {
        return route("upload-service")
                .route(path("/api/uploads/**"), http(fileServiceUrl))
                .filter(pathRewriteFilter("^/api/uploads", "/uploads"))
                .filter(tokenRelayFilter)
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return route("user-service")
                .route(path("/api/users/**"), http(userServiceUrl))
                .filter(pathRewriteFilter("^/api/users", "/api/v1/users"))
                .filter(tokenRelayFilter)
                .build();
    }
}
