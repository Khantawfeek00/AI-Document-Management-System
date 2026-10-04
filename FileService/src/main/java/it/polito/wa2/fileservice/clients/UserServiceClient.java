package it.polito.wa2.fileservice.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserServiceClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate restTemplate;
    private final ServiceAccountTokenProvider serviceAccountTokenProvider;
    private final String userServiceUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            ServiceAccountTokenProvider serviceAccountTokenProvider,
            @Value("${user.service.url:http://localhost:8081}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.serviceAccountTokenProvider = serviceAccountTokenProvider;
        this.userServiceUrl = userServiceUrl;
    }

    private HttpHeaders getHeaders() {
        String token = serviceAccountTokenProvider.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    public String getUserDepartment(String userId) {
        try {
            String url = userServiceUrl + "/api/v1/users/internal/" + userId + "/department";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error fetching user department for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<String> getUsersInDepartment(String department) {
        try {
            String url = userServiceUrl + "/api/v1/users?department={department}&size=1000";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            
            ResponseEntity<PagedUserResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, PagedUserResponse.class, department);
            
            PagedUserResponse body = response.getBody();
            if (body != null && body.content() != null) {
                return body.content().stream().map(UserResponseDTO::id).collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching users for department {}: {}", department, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean userExists(String userId) {
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId;
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserIdByEmail(String email) {
        try {
            String url = userServiceUrl + "/api/v1/users?size=1000";
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            ResponseEntity<PagedUserResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity, PagedUserResponse.class);

            PagedUserResponse body = response.getBody();
            if (body != null && body.content() != null) {
                return body.content().stream()
                        .filter(u -> email.equals(u.email()))
                        .map(UserResponseDTO::id)
                        .findFirst()
                        .orElse(null);
            }
            return null;
        } catch (Exception e) {
            logger.error("Error fetching user by email {}: {}", email, e.getMessage());
            return null;
        }
    }

    public String getUserFullName(String userId) {
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId;
            HttpEntity<Void> entity = new HttpEntity<>(getHeaders());
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("fullName")) {
                return (String) body.get("fullName");
            }
            return null;
        } catch (Exception e) {
            logger.warn("Could not fetch fullName for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
