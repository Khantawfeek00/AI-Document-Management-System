package it.polito.wa2.userdetailservice.entities;

/**
 * Supported application roles.
 * Persisted as a string in the database thanks to EnumType.STRING.
 */
public enum Role {
    ADMIN,
    EDITOR,
    VIEWER
}
