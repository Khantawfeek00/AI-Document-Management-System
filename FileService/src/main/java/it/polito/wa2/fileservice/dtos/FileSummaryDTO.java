package it.polito.wa2.fileservice.dtos;

import java.time.Instant;

public record FileSummaryDTO(
    String id,
    String filename,
    String contentType,
    Long size,
    Instant createdAt,
    Instant updatedAt,
    String status,
    String ownerId,
    String ownerName,
    Boolean isOwner,
    Boolean isShared
) {
    public FileSummaryDTO {
        if (status == null) {
            status = "COMPLETED";
        }
        if (isOwner == null) {
            isOwner = false;
        }
        if (isShared == null) {
            isShared = false;
        }
    }
}
