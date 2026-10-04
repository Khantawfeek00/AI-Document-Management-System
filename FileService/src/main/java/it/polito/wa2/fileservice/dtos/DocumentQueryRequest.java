package it.polito.wa2.fileservice.dtos;

/**
 * Request for RAG query (Step 7)
 * Note: ownerUserId and accessibleFileIds are extracted from JWT and permissions,
 * NOT from user request (for security)
 */
public record DocumentQueryRequest(
    String question,
    String fileId,
    String sensitivity,
    Integer topK
) {
    public DocumentQueryRequest {
        if (topK == null) {
            topK = 5;
        }
    }
}
