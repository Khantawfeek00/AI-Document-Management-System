package it.polito.wa2.userdetailservice.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a system user.
 * Contains identifying information, organizational data and auditing metadata.
 * Annotations drive persistence to a relational database.
 */
@Entity
@Table(name = "users")
public class User {

    // Primary key: UUID
    @Id
    private UUID id;

    // Full name of the user (required)
    @Column(nullable = false)
    private String fullName;

    // Unique email used for identification/login (required)
    @Column(nullable = false, unique = true)
    private String email;

    // Organization the user belongs to (required)
    @Column(nullable = false)
    private String organization;

    // User role; persisted as a string in the DB
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Department or unit, optional
    private String department;

    // Storage quota assigned in bytes, optional
    private Long storageQuota;

    // Free long-form notes; mapped to TEXT to avoid length limits
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Creation timestamp automatically populated by Hibernate
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Last update timestamp automatically populated by Hibernate
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public User() {
    }

    public User(UUID id, String fullName, String email, String organization, Role role, String department, Long storageQuota, String notes) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.organization = organization;
        this.role = role;
        this.department = department;
        this.storageQuota = storageQuota;
        this.notes = notes;
    }

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
