package it.polito.wa2.fileservice.exception;

public class UploadConflictException extends DomainException {
    private final String reason;

    public UploadConflictException(String reason) {
        super("Conflict in upload operation: " + reason, "UPLOAD_CONFLICT");
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
