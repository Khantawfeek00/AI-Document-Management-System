package it.polito.wa2.userdetailservice.keycloak;

import it.polito.wa2.userdetailservice.config.KeycloakProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakClient {

    private final Logger logger = LoggerFactory.getLogger(KeycloakClient.class);
    private final KeycloakProperties props;
    private final WebClient webClient;
    private final String tokenUri;
    private final String adminBase;

    public KeycloakClient(KeycloakProperties props) {
        this.props = props;
        this.webClient = WebClient.builder().baseUrl(props.getServerUrl()).build();
        this.tokenUri = "/realms/" + props.getRealm() + "/protocol/openid-connect/token";
        this.adminBase = "/admin/realms/" + props.getRealm();
        
        logger.info("KeycloakClient initialized with serverUrl={}, realm={}, clientId={}", 
            props.getServerUrl(), props.getRealm(), props.getClientId());
    }

    private String obtainAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());

        Map<String, Object> resp = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(status -> status.isError(), r -> 
                    r.bodyToMono(String.class).map(body -> new RuntimeException("Token error " + r.statusCode() + ": " + body))
                )
                .bodyToMono(Map.class)
                .block();

        if (resp == null) {
            throw new RuntimeException("Keycloak token request failed");
        }

        Object accessToken = resp.get("access_token");
        if (accessToken instanceof String) {
            return (String) accessToken;
        }
        throw new RuntimeException("No access_token from Keycloak");
    }

    public String findUserIdByEmail(String email) {
        String token = obtainAdminToken();
        List<Map<String, Object>> users = webClient.get()
                .uri(uriBuilder -> uriBuilder.path(adminBase + "/users").queryParam("email", email).build())
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (users == null || users.isEmpty()) {
            return null;
        }

        Map<String, Object> first = users.get(0);
        Object id = first.get("id");
        return id instanceof String ? (String) id : null;
    }

    private Map<String, List<String>> sanitizeAttributes(Map<String, Object> raw) {
        if (raw == null) return null;

        Map<String, List<String>> mapped = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();
            List<String> list = new ArrayList<>();
            
            if (v instanceof String) {
                String str = (String) v;
                if (!str.trim().isEmpty()) {
                    list.add(str);
                }
            } else if (v instanceof Collection) {
                for (Object item : (Collection<?>) v) {
                    if (item != null) {
                        String str = item.toString();
                        if (!str.trim().isEmpty()) {
                            list.add(str);
                        }
                    }
                }
            } else if (v != null) {
                String str = v.toString();
                if (!str.trim().isEmpty()) {
                    list.add(str);
                }
            }

            if (!list.isEmpty()) {
                mapped.put(k, list);
            }
        }
        return mapped.isEmpty() ? null : mapped;
    }

    public String createUser(String email, String firstName, String lastName, Map<String, Object> attributes, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required for Keycloak user creation");
        }

        String token = obtainAdminToken();

        Map<String, Object> payload = new HashMap<>();
        payload.put("username", email);
        payload.put("email", email);
        payload.put("enabled", true);

        if (firstName != null && !firstName.trim().isEmpty()) {
            payload.put("firstName", firstName);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            payload.put("lastName", lastName);
        }

        Map<String, List<String>> sanitizedAttrs = sanitizeAttributes(attributes);
        if (sanitizedAttrs != null) {
            payload.put("attributes", sanitizedAttrs);
        }

        String respId = webClient.post()
                .uri(adminBase + "/users")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().is2xxSuccessful() || clientResponse.statusCode().value() == 201) {
                        HttpHeaders headers = clientResponse.headers().asHttpHeaders();
                        java.net.URI location = headers.getLocation();
                        if (location != null && location.getPath() != null) {
                            String[] pathParts = location.getPath().split("/");
                            return Mono.justOrEmpty(pathParts[pathParts.length - 1]);
                        }
                        return Mono.empty();
                    } else {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Keycloak create user failed: status={}, body={}", clientResponse.statusCode(), body);
                                    return Mono.error(new RuntimeException("Keycloak create user error " + clientResponse.statusCode().value() + ": " + body));
                                });
                    }
                })
                .block();

        if (respId == null) {
            throw new RuntimeException("Failed to create user in Keycloak; no id returned");
        }

        if (password != null && !password.trim().isEmpty()) {
            setPassword(respId, password, false);
        }

        return respId;
    }

    public void updateUser(String keycloakId, String email, String firstName, String lastName, Map<String, Object> attributes) {
        String token = obtainAdminToken();
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("enabled", true);
        if (firstName != null) {
            payload.put("firstName", firstName);
        }
        if (lastName != null) {
            payload.put("lastName", lastName);
        }
        if (attributes != null) {
            payload.put("attributes", sanitizeAttributes(attributes));
        }

        // Validate UUID format
        UUID.fromString(keycloakId);

        webClient.put()
                .uri(adminBase + "/users/" + keycloakId)
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.isError(), r ->
                        r.bodyToMono(String.class).map(body -> new RuntimeException("Keycloak update error " + r.statusCode() + ": " + body))
                )
                .toBodilessEntity()
                .block();
    }

    public void setPassword(String keycloakId, String newPassword, boolean temporary) {
        String token = obtainAdminToken();
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "password");
        payload.put("temporary", temporary);
        payload.put("value", newPassword);

        webClient.put()
                .uri(adminBase + "/users/" + keycloakId + "/reset-password")
                .headers(h -> h.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
    
    public void setPassword(String keycloakId, String newPassword) {
        setPassword(keycloakId, newPassword, false);
    }

    public void deleteUser(String keycloakId) {
        String token = obtainAdminToken();
        webClient.delete()
                .uri(adminBase + "/users/" + keycloakId)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
