package it.polito.wa2.fileservice.services;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import it.polito.wa2.fileservice.exception.UploadBadRequestException;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.AbstractMap;
import java.util.Base64;

@Service
public class ChecksumService {

    private final MinioClient minioClient;

    public ChecksumService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public boolean verifyObjectChecksum(String bucket, String objectKey, String checksumHeader) throws Exception {
        var parts = parseChecksumHeader(checksumHeader);
        String algorithm = parts.getKey();
        String expectedBase64 = parts.getValue();
        
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build()
        )) {
            updateDigest(stream, digest);
        }

        String actualBase64 = Base64.getEncoder().encodeToString(digest.digest());
        return actualBase64.equals(expectedBase64);
    }

    public String calculateChecksum(byte[] data, String algorithm) throws Exception {
        String normalizedAlgorithm = switch (algorithm.toUpperCase()) {
            case "SHA1" -> "SHA-1";
            case "SHA256" -> "SHA-256";
            default -> algorithm;
        };

        MessageDigest digest = MessageDigest.getInstance(normalizedAlgorithm);
        digest.update(data);
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    public String calculateChunkChecksum(byte[] chunk, String algorithm) throws Exception {
        String normalizedAlgorithm = switch (algorithm.toUpperCase()) {
            case "SHA1" -> "SHA-1";
            case "SHA256" -> "SHA-256";
            default -> algorithm;
        };

        MessageDigest digest = MessageDigest.getInstance(normalizedAlgorithm);
        digest.update(chunk);
        return Base64.getEncoder().encodeToString(digest.digest());
    }

    public AbstractMap.SimpleEntry<String, String> parseChecksumHeader(String header) {
        if (header == null || header.isBlank()) {
            throw new UploadBadRequestException();
        }

        String[] parts = header.trim().split(" ", 2);

        if (parts.length != 2) {
            throw new UploadBadRequestException("Invalid Upload-Checksum header format. Expected: 'algorithm base64checksum'");
        }

        return new AbstractMap.SimpleEntry<>(parts[0], parts[1]);
    }

    private void updateDigest(InputStream stream, MessageDigest digest) throws Exception {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
    }
}
