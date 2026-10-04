package it.polito.wa2.aiprocessingservice.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.logging.Logger;

@Component
public class ServiceAccountTokenProvider {
    private static final Logger logger = Logger.getLogger(ServiceAccountTokenProvider.class.getName());

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUri;

    private volatile String cachedToken = null;
    private volatile long expiresAt = 0L;
    private final long marginMs = 10_000L; // 10s safety margin

    public ServiceAccountTokenProvider(
            RestTemplate restTemplate,
            @Value("${spring.security.oauth2.client.registration.file-service.client-id:}") String clientIdFromRegistration,
            @Value("${spring.security.oauth2.client.registration.file-service.client-secret:}") String clientSecretFromRegistration,
            @Value("${keycloak.client-id:}") String keycloakClientId,
            @Value("${keycloak.client-secret:}") String keycloakClientSecret,
            @Value("${keycloak.token-uri:}") String tokenUri) {
        this.restTemplate = restTemplate;
        this.tokenUri = tokenUri;

        if (clientIdFromRegistration != null && !clientIdFromRegistration.isBlank()) {
            this.clientId = clientIdFromRegistration;
        } else if (keycloakClientId != null && !keycloakClientId.isBlank()) {
            this.clientId = keycloakClientId;
        } else {
            throw new IllegalArgumentException("No client id configured for ServiceAccountTokenProvider");
        }

        if (clientSecretFromRegistration != null && !clientSecretFromRegistration.isBlank()) {
            this.clientSecret = clientSecretFromRegistration;
        } else if (keycloakClientSecret != null && !keycloakClientSecret.isBlank()) {
            this.clientSecret = keycloakClientSecret;
        } else {
            throw new IllegalArgumentException("No client secret configured for ServiceAccountTokenProvider");
        }
    }

    public String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && now < expiresAt - marginMs) {
            return cachedToken;
        }

        synchronized (this) {
            long nowSync = System.currentTimeMillis();
            if (cachedToken != null && nowSync < expiresAt - marginMs) {
                return cachedToken;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);
            Map<String, Object> respBody = response.getBody();
            if (respBody == null) {
                throw new RuntimeException("Empty token response from Keycloak");
            }

            String accessToken = (String) respBody.get("access_token");
            if (accessToken == null) {
                throw new RuntimeException("No access_token in token response");
            }

            Number expiresInObj = (Number) respBody.get("expires_in");
            long expiresIn = expiresInObj != null ? expiresInObj.longValue() : 60L;
            expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
            cachedToken = accessToken;

            logger.info("Obtained new access token (expires_in=" + expiresIn + "s), clientId=" + clientId);
            return accessToken;
        }
    }
}
