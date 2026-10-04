package it.polito.wa2.fileservice.exception;

public class UploadBadRequestException extends DomainException {
    private final String reason;

    public UploadBadRequestException() {
        this("");
    }

    public UploadBadRequestException(String reason) {
        super("Bad request for upload operation" + (reason != null && !reason.isEmpty() ? ": " + reason : ""), "UPLOAD_BAD_REQUEST");
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
