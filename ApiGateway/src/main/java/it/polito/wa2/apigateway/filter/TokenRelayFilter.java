package it.polito.wa2.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;

@Component
public class TokenRelayFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final Logger logger = LoggerFactory.getLogger(TokenRelayFilter.class);
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public TokenRelayFilter(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        logger.info("=== TokenRelayFilter - Processing request to: {} ===", request.uri());
        logger.info("Authentication type: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");
        logger.info("Is authenticated: {}", authentication != null && authentication.isAuthenticated());

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            logger.info("OAuth2 Authentication found - Client Registration ID: {}", oauthToken.getAuthorizedClientRegistrationId());
            logger.info("Principal name: {}", oauthToken.getName());

            HttpServletRequest servletRequest = request.servletRequest();
            HttpServletResponse servletResponse = (HttpServletResponse) servletRequest.getAttribute("jakarta.servlet.http.HttpServletResponse");

            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(oauthToken.getAuthorizedClientRegistrationId())
                    .principal(oauthToken)
                    .attribute("jakarta.servlet.http.HttpServletRequest", servletRequest)
                    .attribute("jakarta.servlet.http.HttpServletResponse", servletResponse)
                    .build();

            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient != null) {
                String token = authorizedClient.getAccessToken().getTokenValue();
                Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
                Instant now = Instant.now();

                logger.info("✓ Authorized client found - Access token present: {}...", token.substring(0, Math.min(token.length(), 20)));
                logger.info("Token expires at: {}", expiresAt);
                logger.info("Current time: {}", now);

                if (expiresAt != null && expiresAt.isBefore(now)) {
                    logger.error("✗ Token is EXPIRED! This should not happen - refresh failed");
                } else {
                    logger.info("✓ Token is still valid");
                }

                ServerRequest modifiedRequest = ServerRequest.from(request)
                        .header("Authorization", "Bearer " + token)
                        .build();
                logger.info("✓ Adding Authorization header to proxied request");
                return next.handle(modifiedRequest);
            } else {
                logger.error("✗ Authorized client NOT found or could not be refreshed!");
                logger.error("This means the OAuth2 login succeeded but the client was not saved or refresh failed");
            }
        } else {
            logger.warn("✗ Authentication is not OAuth2AuthenticationToken, cannot relay token");
        }

        logger.warn("Proceeding WITHOUT Authorization header");
        return next.handle(request);
    }
}
