package it.polito.wa2.fileservice.services;

import it.polito.wa2.fileservice.clients.UserServiceClient;
import it.polito.wa2.fileservice.dtos.FileShareDTO;
import it.polito.wa2.fileservice.dtos.SharedFileDTO;
import it.polito.wa2.fileservice.entities.File;
import it.polito.wa2.fileservice.entities.FileShare;
import it.polito.wa2.fileservice.entities.FileVersion;
import it.polito.wa2.fileservice.entities.SharePermission;
import it.polito.wa2.fileservice.exception.AccessDeniedException;
import it.polito.wa2.fileservice.exception.FileShareException;
import it.polito.wa2.fileservice.exception.UploadNotFoundException;
import it.polito.wa2.fileservice.repository.FileRepository;
import it.polito.wa2.fileservice.repository.FileShareRepository;
import it.polito.wa2.fileservice.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileShareService {
    private static final Logger logger = LoggerFactory.getLogger(FileShareService.class);

    private final FileShareRepository fileShareRepository;
    private final FileRepository fileRepository;
    private final UserServiceClient userServiceClient;

    public FileShareService(
            FileShareRepository fileShareRepository,
            FileRepository fileRepository,
            UserServiceClient userServiceClient
    ) {
        this.fileShareRepository = fileShareRepository;
        this.fileRepository = fileRepository;
        this.userServiceClient = userServiceClient;
    }

    private boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }

    private boolean isManager() {
        return SecurityUtils.isManager();
    }

    private String currentUserId() {
        return SecurityUtils.getAuthenticatedUserId();
    }

    private Set<String> managerDeptUsers(String userId) {
        String dept = userServiceClient.getUserDepartment(userId);
        if (dept == null) return Collections.emptySet();
        return userServiceClient.getUsersInDepartment(dept).stream().collect(Collectors.toSet());
    }

    @Transactional
    public FileShareDTO createShare(String fileId, String userIdOrEmail, SharePermission permission) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new UploadNotFoundException(fileId));

        String userId = resolveUserId(userIdOrEmail);
        if (userId == null) {
            throw new FileShareException("User with id or email '" + userIdOrEmail + "' does not exist");
        }

        if (fileShareRepository.existsByFile_IdAndSharedWithUserId(fileId, userId)) {
            throw new FileShareException("Share already exists for user " + userId + " on file " + fileId);
        }

        String caller = currentUserId();
        boolean admin = isAdmin();
        boolean manager = isManager();

        boolean canCreate;
        if (admin) {
            canCreate = true;
        } else if (manager) {
            Set<String> deptUsers = managerDeptUsers(caller);
            canCreate = deptUsers.contains(file.getOwnerId());
        } else {
            canCreate = file.getOwnerId().equals(caller);
        }

        if (!canCreate) {
            throw new AccessDeniedException("User " + caller + " is not authorized to create share for file " + fileId);
        }

        FileShare share = new FileShare();
        share.setFile(file);
        share.setSharedWithUserId(userId);
        share.setPermission(permission);

        FileShare saved = fileShareRepository.save(share);
        logger.info("event=share_created fileId={} userId={} permission={}", fileId, userId, permission);

        return it.polito.wa2.fileservice.dtos.Mapper.toDTO(saved);
    }

    private String resolveUserId(String userIdOrEmail) {
        if (isValidUUID(userIdOrEmail)) {
            if (userServiceClient.userExists(userIdOrEmail)) {
                return userIdOrEmail;
            }
            return null;
        } else {
            return userServiceClient.getUserIdByEmail(userIdOrEmail);
        }
    }

    private boolean isValidUUID(String str) {
        try {
            java.util.UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<FileShareDTO> getFileShares(String fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new UploadNotFoundException(fileId));

        String caller = currentUserId();
        boolean admin = isAdmin();
        boolean manager = isManager();

        boolean canView;
        if (admin) {
            canView = true;
        } else if (file.getOwnerId().equals(caller)) {
            canView = true;
        } else if (manager) {
            Set<String> deptUsers = managerDeptUsers(caller);
            canView = deptUsers.contains(file.getOwnerId());
        } else {
            canView = false;
        }

        if (!canView) {
            throw new AccessDeniedException("User " + caller + " is not authorized to view shares for file " + fileId);
        }

        return fileShareRepository.findByFile_Id(fileId).stream()
                .map(it.polito.wa2.fileservice.dtos.Mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SharedFileDTO> getFilesSharedWithUser(String userId) {
        String caller = currentUserId();
        boolean admin = isAdmin();
        boolean manager = isManager();

        List<FileShare> allShares = fileShareRepository.findBySharedWithUserId(userId);

        List<FileShare> filtered;
        if (admin) {
            filtered = allShares;
        } else if (userId.equals(caller)) {
            filtered = allShares;
        } else if (manager) {
            Set<String> deptUsers = managerDeptUsers(caller);
            filtered = allShares.stream()
                    .filter(share -> share.getFile() != null && share.getFile().getOwnerId() != null && deptUsers.contains(share.getFile().getOwnerId()))
                    .collect(Collectors.toList());
        } else {
            filtered = allShares.stream()
                    .filter(share -> share.getFile() != null && caller.equals(share.getFile().getOwnerId()))
                    .collect(Collectors.toList());
        }

        return filtered.stream().map(share -> {
            File file = share.getFile();
            FileVersion latest = file.getVersions().isEmpty() ? null : file.getVersions().get(file.getVersions().size() - 1);
            String ownerName = file.getOwnerId() != null ? userServiceClient.getUserFullName(file.getOwnerId()) : null;

            return new SharedFileDTO(
                    share.getId(),
                    file.getId(),
                    file.getLatestFilename() != null ? file.getLatestFilename() : (latest != null ? latest.getFilename() : null),
                    file.getLatestContentType() != null ? file.getLatestContentType() : (latest != null ? latest.getContentType() : null),
                    latest != null ? latest.getSize() : null,
                    share.getPermission(),
                    share.getCreatedAt(),
                    file.getOwnerId() != null ? file.getOwnerId() : "",
                    ownerName,
                    file.getOwnerId() != null && file.getOwnerId().equals(userId)
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public void deleteShare(String fileId, String shareId) {
        FileShare share = fileShareRepository.findById(shareId)
                .orElseThrow(() -> new FileShareException("File with id " + shareId + " not found"));

        if (share.getFile() == null || !share.getFile().getId().equals(fileId)) {
            throw new FileShareException("Share " + shareId + " does not belong to file " + fileId);
        }

        File file = share.getFile();

        String caller = currentUserId();
        boolean admin = isAdmin();
        boolean manager = isManager();

        boolean canDelete;
        if (admin) {
            canDelete = true;
        } else if (file.getOwnerId().equals(caller)) {
            canDelete = true;
        } else if (manager) {
            Set<String> deptUsers = managerDeptUsers(caller);
            canDelete = deptUsers.contains(file.getOwnerId());
        } else {
            canDelete = false;
        }

        if (!canDelete) {
            throw new AccessDeniedException("User " + caller + " is not authorized to delete share " + shareId + " for file " + fileId);
        }

        fileShareRepository.delete(share);
        logger.info("event=share_deleted fileId={} shareId={}", fileId, shareId);
    }
}
