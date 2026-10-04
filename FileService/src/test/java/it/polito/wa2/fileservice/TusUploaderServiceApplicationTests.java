package it.polito.wa2.fileservice;

import it.polito.wa2.fileservice.config.TestSecurityConfig;
import it.polito.wa2.fileservice.services.UploadCleanUpService;
import it.polito.wa2.fileservice.services.UploadService;
import it.polito.wa2.fileservice.repository.UploadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"}
)
@Import(TestSecurityConfig.class)
@Testcontainers
public class TusUploaderServiceApplicationTests {

    @Container
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test_file")
        .withPassword("test_pass");

    @Container
    public static final GenericContainer<?> minio = new GenericContainer<>("minio/minio:latest")
        .withExposedPorts(9000)
        .withEnv("MINIO_ROOT_USER", "minio")
        .withEnv("MINIO_ROOT_PASSWORD", "minio123")
        .withCommand("server", "/data");

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) postgres.start();
        if (!minio.isRunning()) minio.start();

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("keycloak.base-url", () -> "http://localhost:8080");
        registry.add("keycloak.realm", () -> "test-realm");
        registry.add("keycloak.client-id", () -> "test-client");
        registry.add("keycloak.client-secret", () -> "test-secret");
        registry.add("keycloak.token-uri", () -> "http://localhost:8080/realms/test-realm/protocol/openid-connect/token");

        registry.add("minio.url", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("minio.access-key", () -> "minio");
        registry.add("minio.secret-key", () -> "minio123");
        registry.add("minio.bucket", () -> "uploads");
    }

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UploadCleanUpService uploadCleanUpService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UploadRepository uploadRepository;

    @Test
    public void simulateExpirationAndVerify410Gone() throws Exception {
        var upload = uploadService.createUpload(100L, null);

        try {
            upload.getClass().getMethod("setExpiresAt", Instant.class).invoke(upload, Instant.now().minusSeconds(60));
        } catch (Exception e) {
            try {
                java.lang.reflect.Field field = upload.getClass().getDeclaredField("expiresAt");
                field.setAccessible(true);
                field.set(upload, Instant.now().minusSeconds(60));
            } catch (Exception ex) {}
        }
        uploadRepository.save(upload);

        uploadCleanUpService.cleanupNow();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", "1.0.0");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        String uploadId = "";
        try {
            uploadId = (String) upload.getClass().getMethod("getId").invoke(upload);
        } catch (Exception e) {
            uploadId = (String) upload.getClass().getMethod("id").invoke(upload);
        }

        ResponseEntity<Void> response = restTemplate.exchange("/uploads/" + uploadId, HttpMethod.HEAD, request, Void.class);

        assert response.getStatusCode() == HttpStatus.GONE : "Expected 410 Gone, got " + response.getStatusCode();
    }
}
