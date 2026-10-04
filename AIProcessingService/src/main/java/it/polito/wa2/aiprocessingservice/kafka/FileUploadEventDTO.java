package it.polito.wa2.aiprocessingservice.kafka;

public class FileUploadEventDTO {
    private String fileId;
    private String fileVersionId;
    private String filename;
    private String contentType;
    private String ownerUserId;

    public FileUploadEventDTO() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public String getFileVersionId() { return fileVersionId; }
    public void setFileVersionId(String fileVersionId) { this.fileVersionId = fileVersionId; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }
}
