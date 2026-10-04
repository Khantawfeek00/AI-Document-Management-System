package it.polito.wa2.fileservice.services;

import it.polito.wa2.fileservice.entities.Upload;
import it.polito.wa2.fileservice.entities.UploadStatus;
import it.polito.wa2.fileservice.repository.UploadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class UploadCleanUpService {

    private static final Logger logger = LoggerFactory.getLogger(UploadCleanUpService.class);

    private final UploadService uploadService;
    private final UploadRepository uploadRepository;
    private final StorageService storageService;

    public UploadCleanUpService(
            UploadService uploadService,
            UploadRepository uploadRepository,
            StorageService storageService
    ) {
        this.uploadService = uploadService;
        this.uploadRepository = uploadRepository;
        this.storageService = storageService;
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredUploads() {
        logger.info("Running cleanup of expired uploads");

        Instant now = Instant.now();
        List<Upload> expiredUploads = uploadRepository.findExpiredUploads(now);

        logger.info("{} expired uploads", expiredUploads.size());

        for (Upload upload : expiredUploads) {
            logger.info("Cleaning up expired upload: {}", upload.getId());
            try {
                String objectKey = upload.getObjectKey();
                String bucket = upload.getBucket();

                if (objectKey != null && !objectKey.isBlank()) {
                    storageService.deleteObject(bucket, objectKey);
                } else {
                    logger.info("No objectKey for upload {}, skipping storage delete", upload.getId());
                }

                upload.setStatus(UploadStatus.EXPIRED);
                uploadRepository.save(upload);

                logger.info("Expired upload {} cleaned up successfully", upload.getId());
            } catch (Exception e) {
                logger.error("Error cleaning up expired upload {}: {}", upload.getId(), e.getMessage());
                upload.setStatus(UploadStatus.FAILED);
                uploadRepository.save(upload);
            }
        }
    }

    public void cleanupNow() {
        cleanupExpiredUploads();
    }
}
