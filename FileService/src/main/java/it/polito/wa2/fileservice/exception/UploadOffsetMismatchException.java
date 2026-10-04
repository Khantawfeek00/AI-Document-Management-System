package it.polito.wa2.fileservice.exception;

public class UploadOffsetMismatchException extends DomainException {
    private final String uploadId;
    private final long expected;
    private final long received;

    public UploadOffsetMismatchException(String uploadId, long expected, long received) {
        super("Offset mismatch for upload " + uploadId + ": expected=" + expected + ", received=" + received, "OFFSET_MISMATCH");
        this.uploadId = uploadId;
        this.expected = expected;
        this.received = received;
    }

    public String getUploadId() { return uploadId; }
    public long getExpected() { return expected; }
    public long getReceived() { return received; }
}
