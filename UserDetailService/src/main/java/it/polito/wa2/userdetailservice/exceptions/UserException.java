package it.polito.wa2.userdetailservice.exceptions;

public class UserException extends RuntimeException {
    private final String errorCode;

    public UserException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
