package it.polito.wa2.fileservice;

import it.polito.wa2.fileservice.repository.UploadRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import it.polito.wa2.fileservice.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"})
@Import(TestSecurityConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TusE2ETests {

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
        try {
            if (!postgres.isRunning()) postgres.start();
            if (!minio.isRunning()) minio.start();

            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);

            registry.add("minio.url", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
            registry.add("minio.access-key", () -> "minio");
            registry.add("minio.secret-key", () -> "minio123");
            registry.add("minio.bucket", () -> "uploads");

            registry.add("keycloak.base-url", () -> "http://localhost:8080");
            registry.add("keycloak.realm", () -> "test-realm");
            registry.add("keycloak.client-id", () -> "test-client");
            registry.add("keycloak.client-secret", () -> "test-secret");
            registry.add("keycloak.token-uri", () -> "http://localhost:8080/realms/test-realm/protocol/openid-connect/token");
        } catch (Exception e) {
            throw new IllegalStateException("Errore configurazione test containers", e);
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UploadRepository uploadRepository;

    private String getBasePathFile() {
        return "http://localhost:" + port + "/api/v1/files";
    }

    private String getBasePathBasic() {
        return "http://localhost:" + port + "/uploads";
    }

    private String sha256Base64(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String TUS_VERSION = "1.0.0";

    private String createdUploadId = null;
    private long createdUploadLength = 0L;

    @Test
    @Order(1)
    public void optionRequestReturns204() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response = restTemplate.exchange(getBasePathBasic(), HttpMethod.OPTIONS, entity, String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(response.getHeaders().get("Tus-Version")).isNotEmpty();
        Assertions.assertThat(response.getHeaders().get("Tus-Max-Size")).isNotEmpty();
        Assertions.assertThat(response.getHeaders().get("Access-Control-Allow-Methods")).isNotEmpty();
    }

    @Test
    @Order(2)
    public void postUploadsMissingTusHeaderReturnsInvalidTusVersion() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Upload-Length", "100");
        ResponseEntity<Map> response = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headers), Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    @Order(3)
    public void postUploadsMissingUploadLengthReturns400() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        ResponseEntity<Map> response = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headers), Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(5)
    public void postUploadsCreatesUploadReturns201WithLocationAndUploadExpires() throws Exception {
        long length = 1024L;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        headers.add("Upload-Length", String.valueOf(length));
        headers.add("Upload-Metadata", "filename dGVzdC50eHQ=");
        ResponseEntity<Map> response = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headers), Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            String locStr = response.getHeaders().getFirst("Location");
            if (locStr != null) location = new URI(locStr);
        }
        Assertions.assertThat(location).isNotNull();

        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        createdUploadId = id;
        createdUploadLength = length;
        Assertions.assertThat(response.getHeaders().get("Upload-Expires")).isNotEmpty();
    }

    @Test
    @Order(6)
    public void headExistingUploadReturns204AndHeaders() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        ResponseEntity<Void> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.HEAD, HttpEntity.EMPTY, Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        Assertions.assertThat(response.getHeaders().get("Tus-Resumable")).isNotEmpty();
        Assertions.assertThat(response.getHeaders().get("Upload-Offset")).isNotEmpty();
    }

    @Test
    @Order(7)
    public void headNonExistingUploadReturns404() {
        ResponseEntity<Void> response = restTemplate.exchange(getBasePathBasic() + "/00000000-0000-0000-0000-000000000000", HttpMethod.HEAD, HttpEntity.EMPTY, Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(8)
    public void patchWithMissingTusVersionReturns412() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Upload-Offset", "0");
        headers.add("Content-Type", "application/offset+octet-stream");
        HttpEntity<byte[]> entity = new HttpEntity<>("data".getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    @Order(9)
    public void patchWithWrongContentTypeReturns415() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        headers.add("Upload-Offset", "0");
        headers.add("Content-Type", "application/json");
        HttpEntity<byte[]> entity = new HttpEntity<>("data".getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @Order(10)
    public void patchMissingUploadOffsetReturns409() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        headers.add("Content-Type", "application/offset+octet-stream");
        HttpEntity<byte[]> entity = new HttpEntity<>("abc".getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(11)
    public void patchWithOffsetMismatchReturns409() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        headers.add("Upload-Offset", "1");
        headers.add("Content-Type", "application/offset+octet-stream");
        HttpEntity<byte[]> entity = new HttpEntity<>("hello".getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(12)
    public void patchWithChecksumMismatchReturns400() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        headers.add("Upload-Offset", "0");
        headers.add("Content-Type", "application/offset+octet-stream");
        headers.add("Upload-Checksum", "sha256 deadbeef");
        HttpEntity<byte[]> entity = new HttpEntity<>("some bytes".getBytes(), headers);
        ResponseEntity<String> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, String.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(13)
    public void patchCorrectPathWithCorrectChecksumReturns204AndUpdatesOffset() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        byte[] chunk = new byte[512];
        new Random().nextBytes(chunk);
        String checksum = sha256Base64(chunk);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        headers.add("Upload-Offset", "0");
        headers.add("Content-Type", "application/offset+octet-stream");
        headers.add("Upload-Checksum", "sha256 " + checksum);
        HttpEntity<byte[]> entity = new HttpEntity<>(chunk, headers);
        ResponseEntity<Void> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        String newOffsetStr = response.getHeaders().getFirst("Upload-Offset");
        Assertions.assertThat(newOffsetStr).isNotNull();
        Long newOffset = Long.parseLong(newOffsetStr);
        Assertions.assertThat(newOffset).isGreaterThan(0L);
    }

    @Test
    @Order(14)
    public void patchUntilCompletionThenHEADReturnsFinalOffset() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;

        ResponseEntity<Void> headResp = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.HEAD, HttpEntity.EMPTY, Void.class);
        Assertions.assertThat(headResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        String currentOffsetStr = headResp.getHeaders().getFirst("Upload-Offset");
        long currentOffset = currentOffsetStr != null ? Long.parseLong(currentOffsetStr) : 0L;
        long remaining = Math.max(0, createdUploadLength - currentOffset);
        
        if (remaining > 0) {
            byte[] chunk = new byte[(int) remaining];
            for (int i = 0; i < chunk.length; i++) chunk[i] = 1;
            String checksum = sha256Base64(chunk);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Tus-Resumable", TUS_VERSION);
            headers.add("Upload-Offset", String.valueOf(currentOffset));
            headers.add("Content-Type", "application/offset+octet-stream");
            headers.add("Upload-Checksum", "sha256 " + checksum);
            HttpEntity<byte[]> entity = new HttpEntity<>(chunk, headers);
            ResponseEntity<Void> resp = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, entity, Void.class);

            Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            String finalOffsetStr = resp.getHeaders().getFirst("Upload-Offset");
            Long finalOffset = finalOffsetStr != null ? Long.parseLong(finalOffsetStr) : null;
            Assertions.assertThat(finalOffset).isEqualTo(createdUploadLength);
        }
        ResponseEntity<Void> finalHead = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.HEAD, HttpEntity.EMPTY, Void.class);
        Assertions.assertThat(finalHead.getStatusCode()).isIn(HttpStatus.NO_CONTENT, HttpStatus.GONE, HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(15)
    public void deleteUploadWithWrongTusVersionReturns412() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", "0.0.0");
        ResponseEntity<Void> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.DELETE, new HttpEntity<>(null, headers), Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    @Order(16)
    public void deleteUploadSuccessReturns204() {
        Assertions.assertThat(createdUploadId).isNotNull();
        String id = createdUploadId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        ResponseEntity<Void> response = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.DELETE, new HttpEntity<>(null, headers), Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        ResponseEntity<Void> headAfter = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.HEAD, HttpEntity.EMPTY, Void.class);
        Assertions.assertThat(headAfter.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.GONE);
    }

    @Test
    @Order(17)
    public void listFilesInitiallyReturns200WithPageableStructure() {
        ResponseEntity<Map> response = restTemplate.getForEntity(getBasePathFile(), Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isNotNull();
    }

    private String createId = null;

    @Test
    @Order(18)
    public void getSpecificFileDetailReturns200() throws Exception {
        long length = 256L;
        HttpHeaders headersPost = new HttpHeaders();
        headersPost.add("Tus-Resumable", TUS_VERSION);
        headersPost.add("Upload-Length", String.valueOf(length));
        headersPost.add("Upload-Metadata", "filename ZmlsZTMudHh0");
        ResponseEntity<Map> postResp = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headersPost), Map.class);
        Assertions.assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = postResp.getHeaders().getLocation();
        if (location == null) {
            String locStr = postResp.getHeaders().getFirst("Location");
            if (locStr != null) location = new URI(locStr);
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);

        byte[] chunk = new byte[(int) length];
        for (int i = 0; i < chunk.length; i++) chunk[i] = 3;
        String checksum = sha256Base64(chunk);
        HttpHeaders headersPatch = new HttpHeaders();
        headersPatch.add("Tus-Resumable", TUS_VERSION);
        headersPatch.add("Upload-Offset", "0");
        headersPatch.add("Content-Type", "application/offset+octet-stream");
        headersPatch.add("Upload-Checksum", "sha256 " + checksum);
        ResponseEntity<Void> patchResp = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, new HttpEntity<>(chunk, headersPatch), Void.class);
        Assertions.assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var uploadOpt = uploadRepository.findById(id);
        Assertions.assertThat(uploadOpt.isPresent()).isTrue();
        var upload = uploadOpt.get();
        // Assuming getter for fileId exists
        String fileId = "";
        try {
            fileId = (String) upload.getClass().getMethod("getFileId").invoke(upload);
        } catch (Exception e) {
            fileId = (String) upload.getClass().getMethod("fileId").invoke(upload);
        }
        Assertions.assertThat(fileId).isNotNull();

        createId = fileId;

        ResponseEntity<Map> fileDetailResp = restTemplate.getForEntity(getBasePathFile() + "/" + fileId, Map.class);
        Assertions.assertThat(fileDetailResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(fileDetailResp.getBody()).isNotNull();
        Assertions.assertThat(fileDetailResp.getBody().get("id")).isEqualTo(fileId);
    }

    @Test
    @Order(20)
    public void getNonExistingFileDetailReturns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(getBasePathFile() + "/00000000-0000-0000-0000-000000000000", Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(21)
    public void deleteNonExistingFileReturns404() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Tus-Resumable", TUS_VERSION);
        ResponseEntity<Void> response = restTemplate.exchange(getBasePathFile() + "/00000000-0000-0000-0000-000000000000", HttpMethod.DELETE, new HttpEntity<>(null, headers), Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(22)
    public void deleteExistingFileReturns204() throws Exception {
        long length = 128L;
        HttpHeaders headersPost = new HttpHeaders();
        headersPost.add("Tus-Resumable", TUS_VERSION);
        headersPost.add("Upload-Length", String.valueOf(length));
        headersPost.add("Upload-Metadata", "filename ZmlsZTMudHh0");
        ResponseEntity<Map> postResp = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headersPost), Map.class);
        Assertions.assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        URI location = postResp.getHeaders().getLocation();
        if (location == null) {
            String locStr = postResp.getHeaders().getFirst("Location");
            if (locStr != null) location = new URI(locStr);
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);

        byte[] chunk = new byte[(int) length];
        for (int i = 0; i < chunk.length; i++) chunk[i] = 4;
        String checksum = sha256Base64(chunk);
        HttpHeaders headersPatch = new HttpHeaders();
        headersPatch.add("Tus-Resumable", TUS_VERSION);
        headersPatch.add("Upload-Offset", "0");
        headersPatch.add("Content-Type", "application/offset+octet-stream");
        headersPatch.add("Upload-Checksum", "sha256 " + checksum);
        
        ResponseEntity<Void> patchResp = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, new HttpEntity<>(chunk, headersPatch), Void.class);
        Assertions.assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var uploadOpt = uploadRepository.findById(id);
        Assertions.assertThat(uploadOpt.isPresent()).isTrue();
        var upload = uploadOpt.get();
        String fileId = "";
        try {
            fileId = (String) upload.getClass().getMethod("getFileId").invoke(upload);
        } catch (Exception e) {
            fileId = (String) upload.getClass().getMethod("fileId").invoke(upload);
        }

        HttpHeaders headersDelete = new HttpHeaders();
        headersDelete.add("Tus-Resumable", TUS_VERSION);
        ResponseEntity<Void> deleteResp = restTemplate.exchange(getBasePathFile() + "/" + fileId, HttpMethod.DELETE, new HttpEntity<>(null, headersDelete), Void.class);
        Assertions.assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> getAfter = restTemplate.getForEntity(getBasePathFile() + "/" + fileId, Map.class);
        Assertions.assertThat(getAfter.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(23)
    public void patchOnCompletedUploadReturns409() throws Exception {
        long length = 64L;
        HttpHeaders headersPost = new HttpHeaders();
        headersPost.add("Tus-Resumable", TUS_VERSION);
        headersPost.add("Upload-Length", String.valueOf(length));
        ResponseEntity<Map> postResp = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headersPost), Map.class);
        Assertions.assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = postResp.getHeaders().getLocation();
        if (location == null) {
            String locStr = postResp.getHeaders().getFirst("Location");
            if (locStr != null) location = new URI(locStr);
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);

        byte[] chunk = new byte[(int) length];
        for (int i = 0; i < chunk.length; i++) chunk[i] = 5;
        String checksum = sha256Base64(chunk);
        HttpHeaders headersPatch = new HttpHeaders();
        headersPatch.add("Tus-Resumable", TUS_VERSION);
        headersPatch.add("Upload-Offset", "0");
        headersPatch.add("Content-Type", "application/offset+octet-stream");
        headersPatch.add("Upload-Checksum", "sha256 " + checksum);
        
        ResponseEntity<Void> patchResp = restTemplate.exchange(
            getBasePathBasic() + "/" + id,
            HttpMethod.PATCH,
            new HttpEntity<>(chunk, headersPatch),
            Void.class
        );
        Assertions.assertThat(patchResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        
        HttpHeaders headersPatch2 = new HttpHeaders();
        headersPatch2.add("Tus-Resumable", TUS_VERSION);
        headersPatch2.add("Upload-Offset", String.valueOf(length));
        headersPatch2.add("Content-Type", "application/offset+octet-stream");
        
        ResponseEntity<Void> patchResp2 = restTemplate.exchange(
            getBasePathBasic() + "/" + id,
            HttpMethod.PATCH,
            new HttpEntity<>(chunk, headersPatch2),
            Void.class
        );
        Assertions.assertThat(patchResp2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(24)
    public void getSharesOfNonExistingFileReturn404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(getBasePathFile() + "/00000000-0000-0000-0000-000000000000/shares", Map.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(25)
    public void simulateMinioDownDuringPatchReturns503() throws Exception {
        long length = 128L;
        HttpHeaders headersPost = new HttpHeaders();
        headersPost.add("Tus-Resumable", TUS_VERSION);
        headersPost.add("Upload-Length", String.valueOf(length));
        ResponseEntity<Map> postResp = restTemplate.postForEntity(getBasePathBasic() + "/", new HttpEntity<>(null, headersPost), Map.class);
        Assertions.assertThat(postResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        
        URI location = postResp.getHeaders().getLocation();
        if (location == null) {
            String locStr = postResp.getHeaders().getFirst("Location");
            if (locStr != null) location = new URI(locStr);
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);

        try {
            minio.stop();
        } catch (Exception e) {}

        byte[] chunk = new byte[64];
        for (int i = 0; i < chunk.length; i++) chunk[i] = 2;
        String checksum = sha256Base64(chunk);
        HttpHeaders headersPatch = new HttpHeaders();
        headersPatch.add("Tus-Resumable", TUS_VERSION);
        headersPatch.add("Upload-Offset", "0");
        headersPatch.add("Content-Type", "application/offset+octet-stream");
        headersPatch.add("Upload-Checksum", "sha256 " + checksum);
        
        ResponseEntity<String> resp = restTemplate.exchange(getBasePathBasic() + "/" + id, HttpMethod.PATCH, new HttpEntity<>(chunk, headersPatch), String.class);
        Assertions.assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        try {
            minio.start();
        } catch (Exception e) {}
    }
}
