package it.polito.wa2.fileservice.services;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    private final MinioClient minioClient;

    public StorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void putObject(String bucketName, String objectKey, InputStream data, long size) throws Exception {
        try {
            logger.info("event=put_object bucket={} object={} size={}", bucketName, objectKey, size);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(data, size, -1)
                            .contentType("application/offset+octet-stream")
                            .build()
            );
        } catch (Exception e) {
            logger.error("event=put_object_failed bucket={} object={} error={}", bucketName, objectKey, e.getMessage());
            throw e;
        } finally {
            if (data != null) {
                data.close();
            }
        }
    }

    public InputStream getObject(String bucketName, String objectKey) throws Exception {
        try {
            logger.info("event=get_object bucket={} object={}", bucketName, objectKey);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            logger.error("event=get_object_failed bucket={} object={} error={}", bucketName, objectKey, e.getMessage());
            throw e;
        }
    }

    public void deleteObject(String bucketName, String objectKey) {
        try {
            logger.info("event=delete_object bucket={} object={}", bucketName, objectKey);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            logger.warn("event=delete_object_failed bucket={} object={} error={}", bucketName, objectKey, e.getMessage());
        }
    }

    public void deleteObjects(String bucketName, List<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        logger.info("event=delete_objects bucket={} count={}", bucketName, keys.size());
        try {
            List<DeleteObject> deleteObjects = keys.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteObjects)
                            .build()
            );

            for (Result<DeleteError> result : results) {
                try {
                    DeleteError error = result.get();
                    logger.warn("event=delete_object_error bucket={} error={}", bucketName, error.message());
                } catch (Exception e) {
                    logger.warn("event=delete_object_error bucket={} error={}", bucketName, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warn("event=delete_objects_failed bucket={} error={}", bucketName, e.getMessage());
        }
    }

    public List<String> listObjects(String bucketName, String prefix) {
        List<String> result = new ArrayList<>();
        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> itemResult : objects) {
                result.add(itemResult.get().objectName());
            }
        } catch (Exception e) {
            logger.error("event=list_objects_failed bucket={} prefix={} error={}", bucketName, prefix, e.getMessage());
        }
        return result;
    }

    public Long getObjectSize(String bucketName, String objectKey) {
        try {
            var stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
            return stat.size();
        } catch (Exception e) {
            logger.warn("event=stat_object_failed bucket={} object={} error={}", bucketName, objectKey, e.getMessage());
            return null;
        }
    }

    public void composeObjects(String bucketName, String finalObjectKey, List<String> sourceKeys) throws Exception {
        if (sourceKeys == null || sourceKeys.isEmpty()) {
            logger.warn("event=compose_skipped bucket={} object={} reason=empty_sources", bucketName, finalObjectKey);
            return;
        }
        logger.info("event=compose_objects bucket={} target={} parts={}", bucketName, finalObjectKey, sourceKeys.size());
        try {
            List<ComposeSource> sources = sourceKeys.stream()
                    .map(key -> ComposeSource.builder().bucket(bucketName).object(key).build())
                    .collect(Collectors.toList());

            ComposeObjectArgs args = ComposeObjectArgs.builder()
                    .bucket(bucketName)
                    .object(finalObjectKey)
                    .sources(sources)
                    .build();

            minioClient.composeObject(args);
        } catch (Exception e) {
            logger.error("event=compose_failed bucket={} target={} error={}", bucketName, finalObjectKey, e.getMessage());
            throw e;
        }
    }
}
