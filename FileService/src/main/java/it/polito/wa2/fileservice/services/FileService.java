package it.polito.wa2.fileservice.services;

import it.polito.wa2.fileservice.clients.AiProcessingServiceClient;
import it.polito.wa2.fileservice.clients.UserServiceClient;
import it.polito.wa2.fileservice.dtos.*;
import it.polito.wa2.fileservice.entities.Upload;
import it.polito.wa2.fileservice.entities.UploadStatus;
import it.polito.wa2.fileservice.entities.File;
import it.polito.wa2.fileservice.entities.FileVersion;
import it.polito.wa2.fileservice.exception.AccessDeniedException;
import it.polito.wa2.fileservice.exception.UploadNotFoundException;
import it.polito.wa2.fileservice.exception.FileNotFoundException;
import it.polito.wa2.fileservice.repository.FileRepository;
import it.polito.wa2.fileservice.repository.FileVersionRepository;
import it.polito.wa2.fileservice.repository.UploadRepository;
import it.polito.wa2.fileservice.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final UploadRepository uploadRepository;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final UserServiceClient userServiceClient;
    private final FileVersionRepository fileVersionRepository;
    private final AiProcessingServiceClient aiProcessingServiceClient;

    public FileService(
            UploadRepository uploadRepository,
            FileRepository fileRepository,
            StorageService storageService,
            UserServiceClient userServiceClient,
            FileVersionRepository fileVersionRepository,
            AiProcessingServiceClient aiProcessingServiceClient
    ) {
        this.uploadRepository = uploadRepository;
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.userServiceClient = userServiceClient;
        this.fileVersionRepository = fileVersionRepository;
        this.aiProcessingServiceClient = aiProcessingServiceClient;
    }

    public Page<FileSummaryDTO> getFiles(
            UploadStatus status,
            String contentType,
            String scope,
            Pageable pageable
    ) {
        String userId = SecurityUtils.getAuthenticatedUserId();
        long startTime = System.currentTimeMillis();

        Page<File> filesPage;

        if (SecurityUtils.isAdmin()) {
            if (contentType != null) {
                filesPage = fileRepository.findByLatestContentType(contentType, pageable);
            } else {
                filesPage = fileRepository.findAll(pageable);
            }
        } else if (SecurityUtils.isManager() && "department".equals(scope)) {
            String dept = userServiceClient.getUserDepartment(userId);
            logger.info("event=get_department_files userId={} department={}", userId, dept);
            if (dept != null) {
                List<String> deptUsers = userServiceClient.getUsersInDepartment(dept);
                List<String> allDeptUsers = new ArrayList<>(deptUsers);
                if (!allDeptUsers.contains(userId)) {
                    allDeptUsers.add(userId);
                }
                logger.info("event=get_department_files_users department={} deptUsers={} allDeptUsers={}",
                        dept, deptUsers.size(), allDeptUsers.size());
                logger.info("event=get_department_files_users_detail allDeptUsers={}", allDeptUsers);
                filesPage = fileRepository.findAllByOwnerIdIn(allDeptUsers, pageable);
                logger.info("event=get_department_files_result filesFound={} totalElements={}",
                        filesPage.getContent().size(), filesPage.getTotalElements());
            } else {
                logger.warn("event=get_department_files_no_dept userId={}", userId);
                filesPage = Page.empty(pageable);
            }
        } else if ("personal".equals(scope)) {
            filesPage = fileRepository.findAllByOwnerIdIn(Collections.singletonList(userId), pageable);
        } else if ("shared".equals(scope)) {
            filesPage = fileRepository.findAllSharedWithUser(userId, pageable);
        } else if (SecurityUtils.isManager()) {
            String dept = userServiceClient.getUserDepartment(userId);
            if (dept != null) {
                List<String> deptUsers = userServiceClient.getUsersInDepartment(dept);
                List<String> allDeptUsers = new ArrayList<>(deptUsers);
                if (!allDeptUsers.contains(userId)) {
                    allDeptUsers.add(userId);
                }
                filesPage = fileRepository.findAllVisibleForManager(allDeptUsers, userId, pageable);
            } else {
                filesPage = fileRepository.findAllVisibleForStaff(userId, pageable);
            }
        } else {
            filesPage = fileRepository.findAllVisibleForStaff(userId, pageable);
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info(
                "event=get_files_filesrepo contentTypeFilter={} scope={} page={} size={} totalElements={} durationMs={}",
                contentType, scope, pageable.getPageNumber(), filesPage.getSize(), filesPage.getTotalElements(), duration
        );

        Set<String> ownerIds = filesPage.getContent().stream()
                .map(File::getOwnerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, String> ownerNameMap = new HashMap<>();
        for (String ownerId : ownerIds) {
            try {
                String name = userServiceClient.getUserFullName(ownerId);
                ownerNameMap.put(ownerId, name);
            } catch (Exception e) {
                logger.warn("Could not fetch ownerName for {}: {}", ownerId, e.getMessage());
                ownerNameMap.put(ownerId, null);
            }
        }

        return filesPage.map(file -> {
            String ownerName = file.getOwnerId() != null ? ownerNameMap.get(file.getOwnerId()) : null;
            return it.polito.wa2.fileservice.dtos.Mapper.toSummaryDTO(file, userId, ownerName);
        });
    }

    public FileDetailDTO getFileById(String id) {
        long startTime = System.currentTimeMillis();
        File file = fileRepository.findById(id).orElseThrow(() -> {
            logger.warn("event=get_file_not_found fileId={}", id);
            return new UploadNotFoundException(id);
        });

        long duration = System.currentTimeMillis() - startTime;
        logger.info(
                "event=get_file_success fileId={} versions={} durationMs={}",
                id, file.getVersions().size(), duration
        );
        String userId = SecurityUtils.getAuthenticatedUserId();
        boolean canAccess = SecurityUtils.isAdmin() ||
                (file.getOwnerId() != null && file.getOwnerId().equals(userId)) ||
                file.getShares().stream().anyMatch(share -> share.getSharedWithUserId().equals(userId));

        if (!canAccess) {
            throw new AccessDeniedException("User " + userId + " is not authorized to access file " + id);
        }

        return it.polito.wa2.fileservice.dtos.Mapper.toDetailDTO(file);
    }

    @Transactional
    public FileDetailDTO updateFileMetadata(String id, UpdateFileRequest request) {
        String userId = SecurityUtils.getAuthenticatedUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isManager = SecurityUtils.isManager();

        long startTime = System.currentTimeMillis();
        File file = fileRepository.findById(id).orElseThrow(() -> {
            logger.warn("event=update_file_not_found fileId={}", id);
            return new UploadNotFoundException(id);
        });

        boolean isSharedWithUser = file.getShares().stream().anyMatch(share -> share.getSharedWithUserId().equals(userId));

        boolean canUpdate;
        if (isAdmin) {
            canUpdate = true;
        } else if (file.getOwnerId() != null && file.getOwnerId().equals(userId)) {
            canUpdate = true;
        } else if (isSharedWithUser) {
            canUpdate = true;
        } else if (isManager) {
            String dept = userServiceClient.getUserDepartment(userId);
            if (dept != null) {
                List<String> deptUsers = userServiceClient.getUsersInDepartment(dept);
                canUpdate = deptUsers.contains(file.getOwnerId());
            } else {
                canUpdate = false;
            }
        } else {
            canUpdate = false;
        }

        if (!canUpdate) {
            throw new AccessDeniedException("User " + userId + " is not authorized to update file " + id);
        }

        if (request.filename() != null) {
            logger.info("event=update_file_metadata fileId={} newFilename={}", id, request.filename());
            file.setLatestFilename(request.filename());
            file.setUpdatedAt(java.time.Instant.now());
        }

        File updated = fileRepository.save(file);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=update_file_success fileId={} durationMs={}", id, duration);

        return it.polito.wa2.fileservice.dtos.Mapper.toDetailDTO(updated);
    }

    @Transactional
    public void deleteFile(String id) {
        long startTime = System.currentTimeMillis();
        File file = fileRepository.findById(id).orElseThrow(() -> {
            logger.warn("event=delete_file_not_found fileId={}", id);
            return new UploadNotFoundException(id);
        });

        String userId = SecurityUtils.getAuthenticatedUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isManager = SecurityUtils.isManager();
        boolean isSharedWithUser = file.getShares().stream().anyMatch(share -> share.getSharedWithUserId().equals(userId));

        boolean canDelete;
        if (isAdmin) {
            canDelete = true;
        } else if (file.getOwnerId() != null && file.getOwnerId().equals(userId)) {
            canDelete = true;
        } else if (isSharedWithUser) {
            canDelete = true;
        } else if (isManager) {
            String dept = userServiceClient.getUserDepartment(userId);
            if (dept != null) {
                List<String> deptUsers = userServiceClient.getUsersInDepartment(dept);
                canDelete = deptUsers.contains(file.getOwnerId());
            } else {
                canDelete = false;
            }
        } else {
            canDelete = false;
        }

        if (!canDelete) {
            throw new AccessDeniedException("User " + userId + " is not authorized to delete file " + id);
        }

        try {
            Map<String, List<String>> objectsByBucket = file.getVersions().stream()
                    .collect(Collectors.groupingBy(
                            FileVersion::getBucket,
                            Collectors.mapping(FileVersion::getObjectKey, Collectors.filtering(Objects::nonNull, Collectors.toList()))
                    ));

            objectsByBucket.forEach((bucket, keys) -> {
                if (bucket != null && !bucket.isBlank() && !keys.isEmpty()) {
                    storageService.deleteObjects(bucket, keys);
                    logger.info("event=delete_file_storage fileId={} bucket={} count={}", id, bucket, keys.size());
                }
            });

            fileRepository.delete(file);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("event=delete_file_success fileId={} durationMs={}", id, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("event=delete_file_failed fileId={} error={} durationMs={}", id, e.getMessage(), duration);
            throw e;
        }
    }

    public AbstractMap.SimpleEntry<Upload, InputStream> getFileStream(String id) throws Exception {
        long startTime = System.currentTimeMillis();

        FileVersion targetVersion = fileVersionRepository.findById(id).orElse(null);

        if (targetVersion == null) {
            File file = fileRepository.findById(id).orElseThrow(() -> {
                logger.warn("event=download_file_not_found fileId={}", id);
                return new UploadNotFoundException(id);
            });
            targetVersion = file.getVersions().isEmpty() ? null : file.getVersions().get(file.getVersions().size() - 1);
            if (targetVersion == null) {
                throw new IllegalStateException("No versions for file " + id);
            }
        }

        Upload upload = new Upload();
        upload.setId(targetVersion.getId());
        upload.setFileId(targetVersion.getFile() != null ? targetVersion.getFile().getId() : null);
        upload.setCreatedAt(targetVersion.getUploadedAt());
        upload.setCompletedAt(targetVersion.getUploadedAt());
        upload.setUploadLength(targetVersion.getSize());
        upload.setCurrentOffset(targetVersion.getSize());
        upload.setFilename(targetVersion.getFilename());
        upload.setContentType(targetVersion.getContentType());
        upload.setChecksum(targetVersion.getChecksum());
        upload.setBucket(targetVersion.getBucket());
        upload.setObjectKey(targetVersion.getObjectKey());
        upload.setFinalChecksum(targetVersion.getChecksum());
        upload.setStatus(UploadStatus.COMPLETED);

        String objectKey = upload.getObjectKey();
        if (objectKey == null) {
            throw new IllegalStateException("No objectKey for file " + id);
        }
        InputStream stream = storageService.getObject(upload.getBucket(), objectKey);

        long duration = System.currentTimeMillis() - startTime;
        logger.info(
                "event=download_stream_opened id={} objectKey={} size={} durationMs={}",
                id, objectKey, upload.getUploadLength(), duration
        );
        return new AbstractMap.SimpleEntry<>(upload, stream);
    }

    public String getFilenameForFileId(String fileId) {
        File file = fileRepository.findById(fileId).orElse(null);
        if (file != null && file.getLatestFilename() != null) {
            return file.getLatestFilename();
        } else if (file != null && !file.getVersions().isEmpty()) {
            String fname = file.getVersions().get(file.getVersions().size() - 1).getFilename();
            return fname != null ? fname : "file";
        }
        return "file";
    }

    public String getContentTypeForFileId(String fileId) {
        File file = fileRepository.findById(fileId).orElse(null);
        if (file != null && file.getLatestContentType() != null) {
            return file.getLatestContentType();
        } else if (file != null && !file.getVersions().isEmpty()) {
            String ctype = file.getVersions().get(file.getVersions().size() - 1).getContentType();
            return ctype != null ? ctype : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    @Transactional
    public void saveAiMetadata(String fileVersionId, SetAiMetadataRequest request) {
        FileVersion version = fileVersionRepository.findById(fileVersionId)
                .orElseThrow(() -> new UploadNotFoundException("File version not found: " + fileVersionId));

        version.setSummary(request.summary());
        version.setSensitivity(request.sensitivity());
        version.setTags(new ArrayList<>(request.tags()));

        fileVersionRepository.save(version);
        logger.info("event=ai_metadata_saved fileVersionId={}", fileVersionId);
    }

    public DocumentQueryResponse queryDocuments(DocumentQueryRequest request) {
        String userId = SecurityUtils.getAuthenticatedUserId();
        logger.info("event=query_documents_request userId={} question='{}'", userId, request.question());

        List<String> accessibleFiles = new ArrayList<>();

        if (request.fileId() != null && !request.fileId().isEmpty()) {
            File file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new FileNotFoundException(request.fileId()));
            // Basic ownership/access check (can be expanded based on rules)
            if (!file.getOwnerId().equals(userId) && !file.getSharingRulesJson().contains(userId) && !"PUBLIC".equals(file.getCurrentVersion().getSensitivity())) {
                throw new AccessDeniedException("No access to this file");
            }
            accessibleFiles.add(file.getId());
        } else {
            Page<File> firstPage = fileRepository.findAllVisibleForStaff(userId, Pageable.ofSize(100));
            accessibleFiles.addAll(firstPage.getContent().stream().map(File::getId).collect(Collectors.toList()));

            int currentPage = 1;
            while (currentPage < firstPage.getTotalPages()) {
                Page<File> page = fileRepository.findAllVisibleForStaff(userId, Pageable.ofSize(100).withPage(currentPage));
                accessibleFiles.addAll(page.getContent().stream().map(File::getId).collect(Collectors.toList()));
                currentPage++;
            }
        }

        logger.info("User {} has access to {} files for query", userId, accessibleFiles.size());

        return aiProcessingServiceClient.queryDocuments(userId, accessibleFiles, request);
    }
}
