package it.polito.wa2.fileservice.dtos;

import java.util.List;

/**
 * Request to set AI-generated metadata (Step 6)
 */
public record SetAiMetadataRequest(
    String summary,
    List<String> tags,
    String sensitivity
) {}
