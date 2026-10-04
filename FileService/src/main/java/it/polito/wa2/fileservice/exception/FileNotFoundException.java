package it.polito.wa2.fileservice.exception;

public class FileNotFoundException extends DomainException {
    private final String fileId;

    public FileNotFoundException(String fileId) {
        super("File with id " + fileId + " not found", "FILE_NOT_FOUND");
        this.fileId = fileId;
    }

    public String getFileId() { return fileId; }
}
