package it.polito.wa2.fileservice.exception;

public class UploadStorageException extends DomainException {
    private final String reason;
    private final Throwable storageCause;

    public UploadStorageException(String reason) {
        this(reason, null);
    }

    public UploadStorageException(String reason, Throwable storageCause) {
        super("A storage error occurred during upload operation: " + reason, "STORAGE_UNAVAILABLE", storageCause);
        this.reason = reason;
        this.storageCause = storageCause;
    }

    public String getReason() { return reason; }
    public Throwable getStorageCause() { return storageCause; }
}
