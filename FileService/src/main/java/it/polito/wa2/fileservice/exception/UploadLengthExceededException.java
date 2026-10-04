package it.polito.wa2.fileservice.exception;

public class UploadLengthExceededException extends DomainException {
    private final long provided;
    private final long maxAllowed;

    public UploadLengthExceededException(long provided, long maxAllowed) {
        super("Upload length " + provided + " exceeded the maximum allowed size " + maxAllowed, "UPLOAD_LENGTH_EXCEEDED");
        this.provided = provided;
        this.maxAllowed = maxAllowed;
    }

    public long getProvided() { return provided; }
    public long getMaxAllowed() { return maxAllowed; }
}
