package it.polito.wa2.fileservice.dtos;

import it.polito.wa2.fileservice.entities.File;
import it.polito.wa2.fileservice.entities.FileShare;
import it.polito.wa2.fileservice.entities.FileVersion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Mapper {

    public static FileSummaryDTO toSummaryDTO(File file, String currentUserId, String ownerName) {
        FileVersion latest = null;
        if (file.getVersions() != null && !file.getVersions().isEmpty()) {
            latest = file.getVersions().get(file.getVersions().size() - 1);
        }
        
        boolean isOwner = currentUserId != null && currentUserId.equals(file.getOwnerId());
        boolean isShared = currentUserId != null && !isOwner && file.getShares() != null && 
                           file.getShares().stream().anyMatch(s -> currentUserId.equals(s.getSharedWithUserId()));
                           
        String filename = file.getLatestFilename() != null ? file.getLatestFilename() : (latest != null ? latest.getFilename() : "unknown");
        String contentType = file.getLatestContentType() != null ? file.getLatestContentType() : (latest != null ? latest.getContentType() : "application/octet-stream");
        Long size = latest != null ? latest.getSize() : 0L;
        
        return new FileSummaryDTO(
            file.getId(),
            filename,
            contentType,
            size,
            file.getCreatedAt(),
            file.getUpdatedAt(),
            "COMPLETED",
            file.getOwnerId(),
            ownerName,
            isOwner,
            isShared
        );
    }
    
    public static FileDetailDTO toDetailDTO(File file) {
        FileVersion latest = null;
        if (file.getVersions() != null && !file.getVersions().isEmpty()) {
            latest = file.getVersions().get(file.getVersions().size() - 1);
        }
        
        String filename = file.getLatestFilename() != null ? file.getLatestFilename() : (latest != null ? latest.getFilename() : "unknown");
        String contentType = file.getLatestContentType() != null ? file.getLatestContentType() : (latest != null ? latest.getContentType() : "application/octet-stream");
        Long size = latest != null ? latest.getSize() : 0L;
        
        List<FileVersionDTO> versions = new ArrayList<>();
        if (file.getVersions() != null) {
            versions = file.getVersions().stream()
                .sorted(Comparator.comparingInt(FileVersion::getVersionNumber).reversed())
                .map(Mapper::toDTO)
                .collect(Collectors.toList());
        }
        
        return new FileDetailDTO(
            file.getId(),
            filename,
            contentType,
            size,
            file.getCreatedAt(),
            file.getUpdatedAt(),
            file.getOwnerId(),
            file.getSharingRulesJson(),
            versions
        );
    }
    
    public static FileVersionDTO toDTO(FileVersion version) {
        return new FileVersionDTO(
            version.getId(),
            version.getVersionNumber(),
            version.getFilename(),
            version.getContentType(),
            version.getSize(),
            version.getUploadedAt(),
            version.getSummary(),
            version.getTags() != null ? new ArrayList<>(version.getTags()) : new ArrayList<>(),
            version.getSensitivity(),
            version.getSummary() != null ? "COMPLETED" : "PENDING"
        );
    }
    
    public static FileShareDTO toDTO(FileShare share) {
        return new FileShareDTO(
            share.getId(),
            share.getFile() != null ? share.getFile().getId() : null,
            share.getSharedWithUserId(),
            share.getPermission(),
            share.getCreatedAt(),
            share.getUpdatedAt()
        );
    }
}
