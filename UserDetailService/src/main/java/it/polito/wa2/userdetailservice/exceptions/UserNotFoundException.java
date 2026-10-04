package it.polito.wa2.userdetailservice.exceptions;

import java.util.UUID;

public class UserNotFoundException extends UserException {
    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super("User with id " + userId + " not found", "USER_NOT_FOUND");
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
