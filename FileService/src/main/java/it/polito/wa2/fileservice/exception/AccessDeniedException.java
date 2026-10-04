package it.polito.wa2.fileservice.exception;

public class AccessDeniedException extends DomainException {
    private final String reason;

    public AccessDeniedException(String reason) {
        super("Access denied: " + reason, "ACCESS_DENIED");
        this.reason = reason;
    }

    public String getReason() { return reason; }
}
