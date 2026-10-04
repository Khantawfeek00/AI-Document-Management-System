package it.polito.wa2.fileservice.dtos;

import java.util.List;

/**
 * Response from RAG query (Step 7)
 */
public record DocumentQueryResponse(
    String answer,
    List<DocumentSource> sources
) {}
