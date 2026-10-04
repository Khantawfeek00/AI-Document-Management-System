package it.polito.wa2.aiprocessingservice.clients;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
public class FileServiceClient {
    private final RestTemplate restTemplate;
    private final ServiceAccountTokenProvider tokenProvider;
    private final String baseUrl;

    @org.springframework.beans.factory.annotation.Autowired
    public FileServiceClient(RestTemplate restTemplate, ServiceAccountTokenProvider tokenProvider) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.baseUrl = "http://fileservice";
    }

    public FileServiceClient(RestTemplate restTemplate, ServiceAccountTokenProvider tokenProvider, String baseUrl) {
        this.restTemplate = restTemplate;
        this.tokenProvider = tokenProvider;
        this.baseUrl = baseUrl;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchFileMetadata(String fileId) {
        String token;
        try {
            token = tokenProvider.getAccessToken();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to obtain service account token", e);
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("No access token available");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        String url = baseUrl + "/files/" + fileId;

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        return body != null ? body : Collections.emptyMap();
    }
}
