package it.polito.wa2.userdetailservice.exceptions;

public class EmailAlreadyExistsException extends UserException {
    private final String email;

    public EmailAlreadyExistsException(String email) {
        super("User with email " + email + " already exists", "EMAIL_ALREADY_EXISTS");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
