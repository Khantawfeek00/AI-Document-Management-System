package it.polito.wa2.fileservice.exception;

public class UnsupportedTusMediaTypeException extends DomainException {
    private final String receivedType;

    public UnsupportedTusMediaTypeException(String receivedType) {
        super("Unsupported Content-Type for TUS patch: " + receivedType, "UNSUPPORTED_MEDIA_TYPE");
        this.receivedType = receivedType;
    }

    public String getReceivedType() { return receivedType; }
}
