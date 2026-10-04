package it.polito.wa2.fileservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public ApplicationRunner minioInitializer(MinioClient minioClient) {
        return args -> {
            String bucketName = "uploads";
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!exists) {
                    log.info("Creating MinIO bucket: {}", bucketName);
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("MinIO bucket created successfully: {}", bucketName);
                } else {
                    log.info("MinIO bucket already exists: {}", bucketName);
                }
            } catch (Exception e) {
                log.error("Failed to create MinIO bucket: {}", bucketName, e);
                throw e;
            }
        };
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(minioUrl)
            .credentials(accessKey, secretKey)
            .build();
    }
}
