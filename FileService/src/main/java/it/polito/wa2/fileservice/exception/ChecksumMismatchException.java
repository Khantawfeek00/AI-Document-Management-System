package it.polito.wa2.fileservice.exception;

public class ChecksumMismatchException extends DomainException {
    private final String uploadId;
    private final String expected;
    private final String actual;

    public ChecksumMismatchException(String uploadId, String expected, String actual) {
        super("Checksum verification failed for upload " + uploadId + ". Expected: " + expected + ", Actual: " + actual, "CHECKSUM_MISMATCH");
        this.uploadId = uploadId;
        this.expected = expected;
        this.actual = actual;
    }

    public String getUploadId() { return uploadId; }
    public String getExpected() { return expected; }
    public String getActual() { return actual; }
}
