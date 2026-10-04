package it.polito.wa2.fileservice.dtos;

import java.time.Instant;
import java.util.List;

public record FileVersionDTO(
    String id,
    Integer versionNumber,
    String filename,
    String contentType,
    Long size,
    Instant uploadDate,
    String summary,
    List<String> tags,
    String sensitivity,
    String aiProcessingStatus
) {
    public FileVersionDTO {
        if (tags == null) {
            tags = List.of();
        }
        if (aiProcessingStatus == null) {
            aiProcessingStatus = "PENDING";
        }
    }
}
