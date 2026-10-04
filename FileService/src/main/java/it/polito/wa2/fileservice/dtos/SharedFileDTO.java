package it.polito.wa2.fileservice.dtos;

import it.polito.wa2.fileservice.entities.SharePermission;
import java.time.Instant;

public record SharedFileDTO(
    String shareId,
    String fileId,
    String filename,
    String contentType,
    Long size,
    SharePermission permission,
    Instant sharedAt,
    String ownerId,
    String ownerName,
    Boolean isOwner
) {
    public SharedFileDTO {
        if (isOwner == null) {
            isOwner = false;
        }
    }
}
