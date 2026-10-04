package it.polito.wa2.fileservice.dtos;

import java.time.Instant;
import java.util.List;

public record FileDetailDTO(
    String id,
    String filename,
    String contentType,
    Long size,
    Instant createdAt,
    Instant updatedAt,
    String ownerId,
    String sharingRulesJson,
    List<FileVersionDTO> versions
) {}
