package it.polito.wa2.fileservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.wa2.fileservice.dtos.FileUploadEventDTO;
import it.polito.wa2.fileservice.entities.*;
import it.polito.wa2.fileservice.exception.*;
import it.polito.wa2.fileservice.repository.FileRepository;
import it.polito.wa2.fileservice.repository.FileVersionRepository;
import it.polito.wa2.fileservice.repository.FilesOutboxRepository;
import it.polito.wa2.fileservice.repository.UploadRepository;
import it.polito.wa2.fileservice.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UploadService {
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    private final UploadRepository uploadRepository;
    private final StorageService storageService;
    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FilesOutboxRepository outboxRepository;
    private final String defaultBucket;
    private final ObjectMapper objectMapper;

    public UploadService(
            UploadRepository uploadRepository,
            StorageService storageService,
            FileRepository fileRepository,
            FileVersionRepository fileVersionRepository,
            FilesOutboxRepository outboxRepository,
            @Value("${minio.bucket}") String defaultBucket,
            ObjectMapper objectMapper
    ) {
        this.uploadRepository = uploadRepository;
        this.storageService = storageService;
        this.fileRepository = fileRepository;
        this.fileVersionRepository = fileVersionRepository;
        this.outboxRepository = outboxRepository;
        this.defaultBucket = defaultBucket;
        this.objectMapper = objectMapper;
    }

    private Map<String, String> parseTusMetadata(String header) {
        if (header == null || header.isBlank()) return Collections.emptyMap();

        logger.debug("event=parse_tus_metadata_raw header={}", header);

        Map<String, String> map = Arrays.stream(header.split(","))
                .map(String::trim)
                .map(s -> s.split(" "))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0].trim().toLowerCase(),
                        parts -> {
                            try {
                                return new String(Base64.getDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                logger.warn("Failed to decode metadata value for key {}: {} (err={})", parts[0], parts[1], e.getMessage());
                                return "";
                            }
                        },
                        (v1, v2) -> v2
                ));

        logger.debug("event=parse_tus_metadata_parsed map={}", map);

        return map;
    }

    public Upload createUpload(Long uploadLength, String metadataHeader) {
        return createUpload(uploadLength, metadataHeader, null);
    }

    public Upload createUpload(Long uploadLength, String metadataHeader, String checksumBase64) {
        long startTime = System.currentTimeMillis();
        Map<String, String> metadata = parseTusMetadata(metadataHeader);
        String filename = metadata.get("filename");
        String contentType = metadata.get("contenttype");

        Upload upload = new Upload();
        upload.setFilename(filename);
        upload.setUploadLength(uploadLength);
        upload.setContentType(contentType);
        upload.setStatus(UploadStatus.PENDING);
        upload.setBucket(defaultBucket);
        upload.setChecksum(checksumBase64);

        uploadRepository.save(upload);
        long duration = System.currentTimeMillis() - startTime;
        logger.info(
                "event=upload_created uploadId={} filename={} uploadLength={} contentType={} durationMs={} checksum_provided= {}",
                upload.getId(), filename, uploadLength, contentType, duration, (checksumBase64 != null && !checksumBase64.isBlank())
        );
        return upload;
    }

    public Upload createUploadForFile(String fileId, Long uploadLength, String metadataHeader) {
        return createUploadForFile(fileId, uploadLength, metadataHeader, null);
    }

    public Upload createUploadForFile(String fileId, Long uploadLength, String metadataHeader, String checksumBase64) {
        File file = fileRepository.findById(fileId).orElseThrow(() -> new FileNotFoundException(fileId));

        Upload upload = createUpload(uploadLength, metadataHeader, checksumBase64);
        upload.setFileId(file.getId());
        uploadRepository.save(upload);
        return upload;
    }

    public Upload getUpload(String uploadId) {
        return uploadRepository.findById(uploadId)
                .orElseThrow(() -> new UploadNotFoundException(uploadId));
    }

    @Transactional
    public Long patchUpload(String uploadId, Long uploadOffsetHeader, InputStream chunkStream, Long chunkSize) {
        long startTime = System.currentTimeMillis();
        Upload upload = uploadRepository.findById(uploadId).orElseThrow(() -> new UploadNotFoundException(uploadId));

        if (upload.getStatus() == UploadStatus.COMPLETED) {
            throw new UploadAlreadyCompletedException(uploadId);
        }

        if (upload.getStatus() == UploadStatus.DELETED) {
            throw new UploadGoneException(uploadId);
        }

        Long current = upload.getCurrentOffset();
        if (!uploadOffsetHeader.equals(current)) {
            throw new UploadOffsetMismatchException(uploadId, current, uploadOffsetHeader);
        }

        Integer partIndex = upload.getNextPartIndex();
        String partKey = "uploads/" + upload.getId() + "/parts/" + partIndex;

        logger.info(
                "event=chunk_upload_start uploadId={} partIndex={} chunkSize={} currentOffset={} expectedFinalSize={}",
                upload.getId(), partIndex, chunkSize, current, upload.getUploadLength()
        );

        try {
            storageService.putObject(upload.getBucket(), partKey, chunkStream, chunkSize);

            upload.getPartSizes().add(chunkSize);
            upload.setCurrentOffset(current + chunkSize);
            upload.setNextPartIndex(partIndex + 1);
            upload.setStatus(UploadStatus.UPLOADING);
            uploadRepository.save(upload);

            long duration = System.currentTimeMillis() - startTime;
            logger.info(
                    "event=chunk_accepted uploadId={} partIndex={} chunkSize={} newOffset={} totalParts={} durationMs={}",
                    upload.getId(), partIndex, chunkSize, upload.getCurrentOffset(), upload.getNextPartIndex(), duration
            );

            if (upload.getUploadLength() != null && upload.getCurrentOffset() == upload.getUploadLength()) {
                finalizeUpload(upload);
            }

        } catch (Exception e) {
            upload.setStatus(UploadStatus.FAILED);
            uploadRepository.save(upload);
            long duration = System.currentTimeMillis() - startTime;
            logger.error(
                    "event=chunk_upload_failed uploadId={} partIndex={} error={} durationMs={}",
                    upload.getId(), partIndex, e.getMessage(), duration
            );
            throw new UploadStorageException("Failed to upload chunk at part " + partIndex + ": " + e.getMessage(), e);
        }

        return upload.getCurrentOffset();
    }

    @Transactional
    public void finalizeUpload(Upload upload) {
        long startTime = System.currentTimeMillis();
        List<String> partKeys = IntStream.range(0, upload.getNextPartIndex())
                .mapToObj(i -> "uploads/" + upload.getId() + "/parts/" + i)
                .collect(Collectors.toList());

        boolean isNewFile = upload.getFileId() == null;
        String targetFileId = upload.getFileId() != null ? upload.getFileId() : UUID.randomUUID().toString();

        String finalKey;
        if (isNewFile) {
            finalKey = "uploads/" + targetFileId + "/v1/" + (upload.getFilename() != null ? upload.getFilename() : upload.getId());
        } else {
            File file = fileRepository.findById(upload.getFileId()).orElseThrow(() -> new RuntimeException("File not found: " + upload.getFileId()));
            FileVersion topVersion = fileVersionRepository.findTopByFile_IdOrderByVersionNumberDesc(file.getId()).orElse(null);
            int nextVersion = (topVersion != null ? topVersion.getVersionNumber() : 0) + 1;
            finalKey = "uploads/" + targetFileId + "/v" + nextVersion + "/" + (upload.getFilename() != null ? upload.getFilename() : upload.getId());
        }

        int minComposeSize = 5 * 1024 * 1024; // 5MB

        logger.info(
                "event=compose_start uploadId={} objectKey={} totalParts={} totalSize={} partsData={}",
                upload.getId(), finalKey, partKeys.size(), upload.getUploadLength(),
                upload.getPartSizes().stream().map(String::valueOf).collect(Collectors.joining(","))
        );

        try {
            if (upload.getUploadLength() != null && upload.getUploadLength() < minComposeSize && !partKeys.isEmpty()) {
                logger.warn("event=compose_skipped_small_file uploadId={} size={} minComposeSize={}. Using manual merge.",
                        upload.getId(), upload.getUploadLength(), minComposeSize);

                List<InputStream> streams = new ArrayList<>();
                for (String key : partKeys) {
                    streams.add(storageService.getObject(upload.getBucket(), key));
                }
                SequenceInputStream combinedStream = new SequenceInputStream(Collections.enumeration(streams));
                storageService.putObject(upload.getBucket(), finalKey, combinedStream, upload.getUploadLength());
                upload.setMergeMode(MergeMode.CLIENT_SIDE);

            } else if (!partKeys.isEmpty()) {
                boolean useManualMerge = false;
                for (int i = 0; i < partKeys.size() - 1; i++) {
                    Long size = storageService.getObjectSize(upload.getBucket(), partKeys.get(i));
                    if (size == null || size < minComposeSize) {
                        useManualMerge = true;
                        break;
                    }
                }

                if (useManualMerge) {
                    List<InputStream> streams = new ArrayList<>();
                    long computedTotal = 0;
                    for (String key : partKeys) {
                        streams.add(storageService.getObject(upload.getBucket(), key));
                        Long size = storageService.getObjectSize(upload.getBucket(), key);
                        if (size != null) {
                            computedTotal += size;
                        }
                    }
                    SequenceInputStream combinedStream = new SequenceInputStream(Collections.enumeration(streams));

                    long totalSize;
                    if (upload.getUploadLength() != null) {
                        totalSize = upload.getUploadLength();
                    } else if (computedTotal > 0) {
                        totalSize = computedTotal;
                    } else {
                        totalSize = -1L;
                    }

                    storageService.putObject(upload.getBucket(), finalKey, combinedStream, totalSize);
                    upload.setMergeMode(MergeMode.CLIENT_SIDE);
                } else {
                    storageService.composeObjects(upload.getBucket(), finalKey, partKeys);
                    upload.setMergeMode(MergeMode.SERVER_SIDE);
                }
            }

            // checksum
            long checksumStartTime = System.currentTimeMillis();
            InputStream stream = storageService.getObject(upload.getBucket(), finalKey);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int read;
            while ((read = stream.read(buf)) > 0) {
                digest.update(buf, 0, read);
            }
            stream.close();
            String calculatedChecksum = Base64.getEncoder().encodeToString(digest.digest());
            long checksumDuration = System.currentTimeMillis() - checksumStartTime;

            if (upload.getChecksum() != null && !upload.getChecksum().isBlank() && !upload.getChecksum().equals(calculatedChecksum)) {
                upload.setStatus(UploadStatus.FAILED);
                uploadRepository.save(upload);
                logger.error("event=checksum_mismatch uploadId={} objectKey={} expected={} actual={} durationMs={}",
                        upload.getId(), finalKey, upload.getChecksum(), calculatedChecksum, checksumDuration);
                throw new ChecksumMismatchException(upload.getId(), upload.getChecksum(), calculatedChecksum);
            }

            upload.setObjectKey(finalKey);
            upload.setFinalChecksum(calculatedChecksum);
            upload.setStatus(UploadStatus.COMPLETED);
            upload.setCompletedAt(Instant.now());

            if (isNewFile) {
                String authenticatedUserId = SecurityUtils.getAuthenticatedUserId();
                logger.info("event=create_new_file uploadId={} fileId={} ownerId={}", upload.getId(), targetFileId, authenticatedUserId);
                File newFile = new File();
                newFile.setId(targetFileId);
                newFile.setOwnerId(authenticatedUserId);
                newFile.setCreatedAt(Instant.now());

                File savedFile = fileRepository.save(newFile);
                fileRepository.flush();

                FileVersion version = new FileVersion();
                version.setFile(savedFile);
                version.setVersionNumber(1);
                String resolvedFilename = upload.getFilename() != null ? upload.getFilename() : "unnamed";
                logger.info("event=create_version_filename uploadId={} resolvedFilename={}", upload.getId(), resolvedFilename);
                version.setFilename(resolvedFilename);
                version.setSize(upload.getUploadLength() != null ? upload.getUploadLength() : 0L);
                version.setContentType(upload.getContentType());
                version.setChecksum(calculatedChecksum);
                version.setObjectKey(finalKey);
                version.setBucket(upload.getBucket());
                version.setUploadedAt(Instant.now());

                FileVersion savedVersion = fileVersionRepository.save(version);

                try {
                    String resolvedContentType = savedVersion.getContentType() != null ? savedVersion.getContentType() : upload.getContentType();
                    String ownerIdStr = savedFile.getOwnerId();
                    if (ownerIdStr != null && resolvedContentType != null && resolvedContentType.toLowerCase().startsWith("text/") || resolvedContentType.equalsIgnoreCase("application/pdf")) {
                        Map<String, Object> payloadMap = new HashMap<>();
                        payloadMap.put("fileVersionId", UUID.fromString(savedVersion.getId()));
                        payloadMap.put("fileId", savedFile.getId());
                        payloadMap.put("ownerUserId", ownerIdStr);
                        payloadMap.put("filename", savedVersion.getFilename() != null ? savedVersion.getFilename() : (upload.getFilename() != null ? upload.getFilename() : "unnamed"));
                        payloadMap.put("contentType", resolvedContentType);

                        String payload = objectMapper.writeValueAsString(payloadMap);

                        FilesOutboxEvent event = new FilesOutboxEvent();
                        event.setAggregateType("FileVersion");
                        event.setAggregateId(UUID.fromString(savedVersion.getId()));
                        event.setEventType("FileUploadCompleted");
                        event.setPayload(payload);

                        outboxRepository.save(event);
                        logger.info("event=file_upload_event_sent uploadId={} fileVersionId={} fileId={}", upload.getId(), savedVersion.getId(), savedFile.getId());
                    }
                } catch (Exception e) {
                    logger.error("event=file_upload_event_not_sent uploadId={} error={}", upload.getId(), e.getMessage(), e);
                }

                if (savedFile.getVersions() == null) {
                    savedFile.setVersions(new ArrayList<>());
                }
                savedFile.getVersions().add(savedVersion);
                savedFile.setCurrentVersion(savedVersion);
                savedFile.setLatestFilename(savedVersion.getFilename());
                savedFile.setLatestContentType(savedVersion.getContentType());
                savedFile.setLatestChecksum(savedVersion.getChecksum());
                savedFile.setUpdatedAt(Instant.now());
                savedFile.setLatestSize(savedVersion.getSize());
                fileRepository.save(savedFile);

                upload.setFileId(savedFile.getId());
                uploadRepository.save(upload);
            } else {
                File existingfile = fileRepository.findById(upload.getFileId()).orElseThrow(() -> new RuntimeException("File not found: " + upload.getFileId()));
                FileVersion topVersion = fileVersionRepository.findTopByFile_IdOrderByVersionNumberDesc(existingfile.getId()).orElse(null);
                int nextVersionNumber = (topVersion != null ? topVersion.getVersionNumber() : 0) + 1;

                FileVersion version = new FileVersion();
                version.setFile(existingfile);
                version.setVersionNumber(nextVersionNumber);

                String resolvedFilename = upload.getFilename() != null ? upload.getFilename() : "unnamed";
                logger.info("event=create_version_filename uploadId={} resolvedFilename={}", upload.getId(), resolvedFilename);
                version.setFilename(resolvedFilename);
                version.setSize(upload.getUploadLength() != null ? upload.getUploadLength() : 0L);
                version.setContentType(upload.getContentType());
                version.setChecksum(calculatedChecksum);
                version.setObjectKey(finalKey);
                version.setBucket(upload.getBucket());
                version.setUploadedAt(Instant.now());

                FileVersion savedVersion = fileVersionRepository.save(version);

                try {
                    String resolvedContentType = savedVersion.getContentType() != null ? savedVersion.getContentType() : upload.getContentType();
                    String ownerIdStr = existingfile.getOwnerId();
                    if (ownerIdStr != null && resolvedContentType != null && resolvedContentType.toLowerCase().startsWith("text/") || resolvedContentType.equalsIgnoreCase("application/pdf")) {
                        Map<String, Object> payloadMap = new HashMap<>();
                        payloadMap.put("fileVersionId", UUID.fromString(savedVersion.getId()));
                        payloadMap.put("fileId", existingfile.getId());
                        payloadMap.put("ownerUserId", ownerIdStr);
                        payloadMap.put("filename", savedVersion.getFilename() != null ? savedVersion.getFilename() : (upload.getFilename() != null ? upload.getFilename() : "unnamed"));
                        payloadMap.put("contentType", resolvedContentType);

                        String payload = objectMapper.writeValueAsString(payloadMap);

                        FilesOutboxEvent event = new FilesOutboxEvent();
                        event.setAggregateType("FileVersion");
                        event.setAggregateId(UUID.fromString(savedVersion.getId()));
                        event.setEventType("FileUploadCompleted");
                        event.setPayload(payload);

                        outboxRepository.save(event);
                        logger.info("event=file_upload_event_sent uploadId={} fileVersionId={} fileId={}", upload.getId(), savedVersion.getId(), existingfile.getId());
                    }
                } catch (Exception e) {
                    logger.warn("event=file_upload_event_failed uploadId={} error={}", upload.getId(), e.getMessage());
                }

                existingfile.setCurrentVersion(savedVersion);
                existingfile.setLatestFilename(savedVersion.getFilename());
                existingfile.setLatestContentType(savedVersion.getContentType());
                existingfile.setLatestChecksum(savedVersion.getChecksum());
                existingfile.setUpdatedAt(Instant.now());
                existingfile.setLatestSize(savedVersion.getSize());
                fileRepository.save(existingfile);
            }

            uploadRepository.save(upload);
            storageService.deleteObjects(upload.getBucket(), partKeys);

            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info(
                    "event=upload_completed uploadId={} objectKey={} finalSize={} checksum={} mergeMode={} totalParts={} checksumDurationMs={} totalDurationMs={} checksumVerified={}",
                    upload.getId(), finalKey, upload.getUploadLength(), calculatedChecksum, upload.getMergeMode(),
                    partKeys.size(), checksumDuration, totalDuration, upload.getChecksum() != null
            );

        } catch (Exception e) {
            upload.setStatus(UploadStatus.FAILED);
            uploadRepository.save(upload);
            long duration = System.currentTimeMillis() - startTime;
            logger.error("event=compose_failed uploadId={} objectKey={} error={} durationMs={}", upload.getId(), finalKey, e.getMessage(), duration);
            throw new UploadStorageException("Failed to finalize upload: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteUpload(String uploadId) {
        long startTime = System.currentTimeMillis();
        Upload upload = uploadRepository.findById(uploadId).orElseThrow(() -> new UploadNotFoundException(uploadId));
        logger.info("event=upload_delete_start uploadId={} status={} objectKey={}", upload.getId(), upload.getStatus(), upload.getObjectKey());
        try {
            String bucket = upload.getBucket();
            String partPrefix = "uploads/" + upload.getId() + "/parts/";
            String finalPrefix = "uploads/" + upload.getId() + "/final/";

            List<String> partKeys = storageService.listObjects(bucket, partPrefix);
            if (!partKeys.isEmpty()) {
                storageService.deleteObjects(bucket, partKeys);
                long totalSize = upload.getPartSizes().stream().mapToLong(Long::longValue).sum();
                logger.info("event=upload_delete_parts uploadId={} count={} totalSize={}", upload.getId(), partKeys.size(), totalSize);
            }
            List<String> finalKeys = storageService.listObjects(bucket, finalPrefix);
            if (!finalKeys.isEmpty()) {
                storageService.deleteObjects(bucket, finalKeys);
                logger.info("event=upload_delete_final uploadId={} count={}", upload.getId(), finalKeys.size());
            }
            upload.setStatus(UploadStatus.DELETED);
            uploadRepository.save(upload);
            long duration = System.currentTimeMillis() - startTime;
            logger.info(
                    "event=upload_deleted uploadId={} durationMs={} partsCleaned={} finalsCleaned={}",
                    upload.getId(), duration, partKeys.size(), finalKeys.size()
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("event=upload_delete_failed uploadId={} error={} durationMs={}", uploadId, e.getMessage(), duration);
            throw new UploadStorageException("Failed to delete upload: " + e.getMessage(), e);
        }
    }

    public boolean verifyChunkChecksum(byte[] chunk, String expectedBase64) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(chunk);
            String actualBase64 = Base64.getEncoder().encodeToString(digest.digest());
            return actualBase64.equals(expectedBase64);
        } catch (Exception e) {
            return false;
        }
    }
}
