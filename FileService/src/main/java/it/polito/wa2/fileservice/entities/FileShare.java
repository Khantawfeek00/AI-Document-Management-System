package it.polito.wa2.fileservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_shares")
public class FileShare {
    @Id
    private String id = UUID.randomUUID().toString();

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id")
    private File file;

    @Column(nullable = false)
    private String sharedWithUserId = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermission permission = SharePermission.READ;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public FileShare() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public File getFile() { return file; }
    public void setFile(File file) { this.file = file; }

    public String getSharedWithUserId() { return sharedWithUserId; }
    public void setSharedWithUserId(String sharedWithUserId) { this.sharedWithUserId = sharedWithUserId; }

    public SharePermission getPermission() { return permission; }
    public void setPermission(SharePermission permission) { this.permission = permission; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
