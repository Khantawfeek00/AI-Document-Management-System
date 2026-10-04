package it.polito.wa2.fileservice.exception;

public class UploadAlreadyCompletedException extends DomainException {
    private final String uploadId;

    public UploadAlreadyCompletedException(String uploadId) {
        super("Upload " + uploadId + " already completed", "UPLOAD_ALREADY_COMPLETED");
        this.uploadId = uploadId;
    }

    public String getUploadId() { return uploadId; }
}
