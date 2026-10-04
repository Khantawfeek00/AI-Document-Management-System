package it.polito.wa2.fileservice.exception;

public class FileShareException extends DomainException {
    private final String reason;

    public FileShareException(String reason) {
        super("File share error: " + reason, "FILE_SHARE_ERROR");
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
