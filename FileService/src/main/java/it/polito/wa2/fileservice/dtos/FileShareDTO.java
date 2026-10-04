package it.polito.wa2.fileservice.dtos;

import it.polito.wa2.fileservice.entities.SharePermission;
import java.time.Instant;

public record FileShareDTO(
    String id,
    String fileId,
    String sharedWithUserId,
    SharePermission permission,
    Instant createdAt,
    Instant updatedAt
) {}
