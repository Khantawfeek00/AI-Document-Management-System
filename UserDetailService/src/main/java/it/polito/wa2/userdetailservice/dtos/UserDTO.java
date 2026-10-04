package it.polito.wa2.userdetailservice.dtos;

import it.polito.wa2.userdetailservice.entities.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for exposing User details.
 * This is returned by the API.
 */
public class UserDTO {
    private UUID id;
    private String fullName;
    private String email;
    private String organization;
    private Role role;
    private String department;
    private Long storageQuota;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public UserDTO(UUID id, String fullName, String email, String organization, Role role, String department, Long storageQuota, String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.organization = organization;
        this.role = role;
        this.department = department;
        this.storageQuota = storageQuota;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getStorageQuota() {
        return storageQuota;
    }

    public void setStorageQuota(Long storageQuota) {
        this.storageQuota = storageQuota;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
