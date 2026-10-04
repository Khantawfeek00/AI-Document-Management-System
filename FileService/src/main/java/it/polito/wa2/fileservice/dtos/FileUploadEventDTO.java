package it.polito.wa2.fileservice.dtos;

import java.util.UUID;

public record FileUploadEventDTO(
    UUID fileVersionId,
    UUID fileId,
    UUID ownerUserId,
    String filename,
    String contentType
) {}
