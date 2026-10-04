package it.polito.wa2.fileservice.dtos;

/**
 * Document source used in RAG response (Step 7)
 */
public record DocumentSource(
    String fileVersionId,
    String filename,
    String chunkContent,
    Double similarity
) {}
