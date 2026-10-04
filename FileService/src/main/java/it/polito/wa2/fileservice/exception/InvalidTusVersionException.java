package it.polito.wa2.fileservice.exception;

public class InvalidTusVersionException extends DomainException {
    private final String receivedVersion;
    private final String supportedVersion;

    public InvalidTusVersionException(String receivedVersion, String supportedVersion) {
        super("Invalid Tus-Resumable header: received=" + receivedVersion + ", supported=" + supportedVersion, "INVALID_TUS_VERSION");
        this.receivedVersion = receivedVersion;
        this.supportedVersion = supportedVersion;
    }

    public String getReceivedVersion() { return receivedVersion; }
    public String getSupportedVersion() { return supportedVersion; }
}
