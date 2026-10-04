package it.polito.wa2.fileservice.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.JoinColumn;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class File {
    @Id
    private String id = UUID.randomUUID().toString();

    private String ownerId;

    private String latestFilename;
    private String latestContentType;
    private String latestChecksum;
    private Long latestSize;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String sharingRulesJson;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNumber ASC")
    private List<FileVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FileShare> shares = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "current_version_id")
    private FileVersion currentVersion;

    public File() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getLatestFilename() { return latestFilename; }
    public void setLatestFilename(String latestFilename) { this.latestFilename = latestFilename; }

    public String getLatestContentType() { return latestContentType; }
    public void setLatestContentType(String latestContentType) { this.latestContentType = latestContentType; }

    public String getLatestChecksum() { return latestChecksum; }
    public void setLatestChecksum(String latestChecksum) { this.latestChecksum = latestChecksum; }

    public Long getLatestSize() { return latestSize; }
    public void setLatestSize(Long latestSize) { this.latestSize = latestSize; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getSharingRulesJson() { return sharingRulesJson; }
    public void setSharingRulesJson(String sharingRulesJson) { this.sharingRulesJson = sharingRulesJson; }

    public List<FileVersion> getVersions() { return versions; }
    public void setVersions(List<FileVersion> versions) { this.versions = versions; }

    public List<FileShare> getShares() { return shares; }
    public void setShares(List<FileShare> shares) { this.shares = shares; }

    public FileVersion getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(FileVersion currentVersion) { this.currentVersion = currentVersion; }
}
