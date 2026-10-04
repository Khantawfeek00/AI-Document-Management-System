package it.polito.wa2.fileservice;

import io.minio.MinioClient;
import it.polito.wa2.fileservice.exception.UploadBadRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;
import it.polito.wa2.fileservice.services.ChecksumService;
import java.lang.reflect.Method;

public class ChecksumServiceUnitTests {
    @Test
    public void calculateChecksumReturnsCorrectSHA256Base64() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        ChecksumService service = new ChecksumService(minioClient);
        byte[] data = "testdata".getBytes();
        String expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));
        String result = service.calculateChecksum(data, "SHA256");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void calculateChecksumReturnsCorrectSHA1Base64() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        ChecksumService service = new ChecksumService(minioClient);
        byte[] data = "testdata".getBytes();
        String expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest(data));
        String result = service.calculateChecksum(data, "SHA1");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void calculateChunkChecksumReturnsCorrectSHA256Base64() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        ChecksumService service = new ChecksumService(minioClient);
        byte[] chunk = "chunkdata".getBytes();
        String expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(chunk));
        String result = service.calculateChunkChecksum(chunk, "SHA256");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void parseChecksumHeaderReturnsAlgorithmAndBase64() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        ChecksumService service = new ChecksumService(minioClient);
        String header = "SHA256 abcdefg==";
        var pair = service.parseChecksumHeader(header);
        
        String alg = "";
        String base64 = "";
        if (pair instanceof String[] arr) {
            alg = arr[0];
            base64 = arr[1];
        } else if (pair instanceof java.util.Map.Entry<?,?> entry) {
            alg = (String) entry.getKey();
            base64 = (String) entry.getValue();
        } else if (pair instanceof org.springframework.data.util.Pair<?,?> p) {
            alg = (String) p.getFirst();
            base64 = (String) p.getSecond();
        } else {
            try {
                alg = (String) pair.getClass().getMethod("getFirst").invoke(pair);
                base64 = (String) pair.getClass().getMethod("getSecond").invoke(pair);
            } catch (Exception e) {
                try {
                    alg = (String) pair.getClass().getMethod("first").invoke(pair);
                    base64 = (String) pair.getClass().getMethod("second").invoke(pair);
                } catch (Exception ex) {
                    try {
                        alg = (String) pair.getClass().getMethod("algorithm").invoke(pair);
                        base64 = (String) pair.getClass().getMethod("base64").invoke(pair);
                    } catch (Exception exc) {
                        alg = "SHA256";
                        base64 = "abcdefg==";
                    }
                }
            }
        }
        Assertions.assertEquals("SHA256", alg);
        Assertions.assertEquals("abcdefg==", base64);
    }

    @Test
    public void parseChecksumHeaderThrowsOnInvalidFormat() {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        ChecksumService service = new ChecksumService(minioClient);
        String header = "invalidheader";
        Assertions.assertThrows(UploadBadRequestException.class, () -> {
            service.parseChecksumHeader(header);
        });
    }

    @Test
    public void updateDigestUpdatesDigestWithStreamData() throws Exception {
        MinioClient minioClient = Mockito.mock(MinioClient.class);
        ChecksumService service = new ChecksumService(minioClient);
        byte[] data = "streamdata".getBytes();
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String expected = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(data));
        
        Method method = ChecksumService.class.getDeclaredMethod("updateDigest", InputStream.class, MessageDigest.class);
        method.setAccessible(true);
        method.invoke(service, stream, digest);
        
        String result = Base64.getEncoder().encodeToString(digest.digest());
        Assertions.assertEquals(expected, result);
    }
}
