package it.polito.wa2.fileservice.dtos;

import java.time.Instant;
import java.util.List;

public record UploadDTO(
    String id,
    String fileId,
    Long uploadLength,
    Long currentOffset,
    String checksum,
    Instant completedAt,
    Instant createdAt,
    Instant expiresAt,
    String status,
    Integer chunkCount,
    String mergeMode,
    List<Long> partSizes
) {}
