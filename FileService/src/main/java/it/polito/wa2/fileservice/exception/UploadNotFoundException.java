package it.polito.wa2.fileservice.exception;

public class UploadNotFoundException extends DomainException {
    private final String uploadId;

    public UploadNotFoundException(String uploadId) {
        super("Upload with id " + uploadId + " not found", "UPLOAD_NOT_FOUND");
        this.uploadId = uploadId;
    }

    public String getUploadId() {
        return uploadId;
    }
}
