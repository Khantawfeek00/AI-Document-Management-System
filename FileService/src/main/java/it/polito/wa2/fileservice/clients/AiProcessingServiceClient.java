package it.polito.wa2.fileservice.clients;

import it.polito.wa2.fileservice.dtos.DocumentQueryRequest;
import it.polito.wa2.fileservice.dtos.DocumentQueryResponse;
import it.polito.wa2.fileservice.dtos.DocumentSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiProcessingServiceClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;
    private final ServiceAccountTokenProvider serviceAccountTokenProvider;
    private final String aiServiceUrl;

    public AiProcessingServiceClient(
            RestTemplate restTemplate,
            ServiceAccountTokenProvider serviceAccountTokenProvider,
            @Value("${ai.processing.service.url:http://localhost:8084}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.serviceAccountTokenProvider = serviceAccountTokenProvider;
        this.aiServiceUrl = aiServiceUrl;
    }

    private HttpHeaders getHeaders() {
        String token = serviceAccountTokenProvider.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    public DocumentQueryResponse queryDocuments(String ownerUserId, List<String> accessibleFileIds, DocumentQueryRequest request) {
        logger.info("Calling AIProcessingService RAG: userId={}, accessibleFiles={}, question='{}'", ownerUserId, accessibleFileIds.size(), request.question());

        try {
            String url = aiServiceUrl + "/api/v1/rag/query";

            Map<String, Object> requestWithContext = new HashMap<>();
            requestWithContext.put("ownerUserId", ownerUserId);
            requestWithContext.put("accessibleFileIds", accessibleFileIds);
            requestWithContext.put("question", request.question());
            requestWithContext.put("sensitivity", request.sensitivity());
            requestWithContext.put("topK", request.topK());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestWithContext, getHeaders());
            ResponseEntity<DocumentQueryResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    DocumentQueryResponse.class
            );

            DocumentQueryResponse body = response.getBody();
            if (body != null) {
                logger.info("RAG query successful: {} sources", body.sources() != null ? body.sources().size() : 0);
                return body;
            } else {
                return new DocumentQueryResponse("No response from AI service", Collections.emptyList());
            }

        } catch (Exception e) {
            logger.error("Failed to query AIProcessingService: {}", e.getMessage(), e);
            return new DocumentQueryResponse("Error querying documents: " + e.getMessage(), Collections.emptyList());
        }
    }
}
