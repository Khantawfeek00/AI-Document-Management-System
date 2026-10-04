package it.polito.wa2.fileservice.exception;

public class InvalidOffsetException extends DomainException {
    private final Long expected;
    private final Long received;

    public InvalidOffsetException(Long expected, Long received) {
        super("Invalid upload offset: expected=" + expected + ", received=" + received, "INVALID_OFFSET");
        this.expected = expected;
        this.received = received;
    }

    public Long getExpected() { return expected; }
    public Long getReceived() { return received; }
}
