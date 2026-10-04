package it.polito.wa2.fileservice.exception;

public class UploadGoneException extends DomainException {
    private final String uploadId;

    public UploadGoneException() {
        this(null);
    }

    public UploadGoneException(String uploadId) {
        super("Upload " + (uploadId != null ? "(" + uploadId + ") " : "") + "has been removed or is no longer available", "UPLOAD_GONE");
        this.uploadId = uploadId;
    }

    public String getUploadId() { return uploadId; }
}
