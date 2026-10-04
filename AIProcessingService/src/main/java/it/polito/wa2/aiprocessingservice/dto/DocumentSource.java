package it.polito.wa2.aiprocessingservice.dto;

public class DocumentSource {
    private String fileVersionId;
    private String filename;
    private String chunkContent;
    private Double similarity;

    public DocumentSource() {}

    public DocumentSource(String fileVersionId, String filename, String chunkContent, Double similarity) {
        this.fileVersionId = fileVersionId;
        this.filename = filename;
        this.chunkContent = chunkContent;
        this.similarity = similarity;
    }

    public String getFileVersionId() { return fileVersionId; }
    public void setFileVersionId(String fileVersionId) { this.fileVersionId = fileVersionId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getChunkContent() { return chunkContent; }
    public void setChunkContent(String chunkContent) { this.chunkContent = chunkContent; }
    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
}
