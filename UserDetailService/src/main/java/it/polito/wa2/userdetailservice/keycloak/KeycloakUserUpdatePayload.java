package it.polito.wa2.userdetailservice.keycloak;

import java.util.List;
import java.util.Map;

public class KeycloakUserUpdatePayload {
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private Map<String, List<String>> attributes;

    public KeycloakUserUpdatePayload() {
    }

    public KeycloakUserUpdatePayload(String firstName, String lastName, String email, Boolean enabled, Map<String, List<String>> attributes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.enabled = enabled;
        this.attributes = attributes;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }
}
