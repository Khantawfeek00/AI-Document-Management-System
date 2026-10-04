package it.polito.wa2.fileservice.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "uploads")
public class Upload {
    @Id
    private String id = UUID.randomUUID().toString();

    private String fileId;

    private Instant createdAt = Instant.now();
    private Instant completedAt;
    private Instant expiresAt = Instant.now().plusSeconds(24 * 3600);

    private Long uploadLength;

    private long currentOffset = 0L;

    private String filename;
    private String contentType;
    private String checksum;

    private String bucket;
    private String objectKey;
    private String finalChecksum;

    @ElementCollection
    @CollectionTable(name = "upload_part_sizes", joinColumns = @JoinColumn(name = "upload_id"))
    @OrderColumn(name = "part_index")
    private List<Long> partSizes = new ArrayList<>();

    private int nextPartIndex = 0;

    @Enumerated(EnumType.STRING)
    private UploadStatus status = UploadStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private MergeMode mergeMode;

    @Version
    private Long version;

    public Upload() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getUploadLength() { return uploadLength; }
    public void setUploadLength(Long uploadLength) { this.uploadLength = uploadLength; }

    public long getCurrentOffset() { return currentOffset; }
    public void setCurrentOffset(long currentOffset) { this.currentOffset = currentOffset; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getFinalChecksum() { return finalChecksum; }
    public void setFinalChecksum(String finalChecksum) { this.finalChecksum = finalChecksum; }

    public List<Long> getPartSizes() { return partSizes; }
    public void setPartSizes(List<Long> partSizes) { this.partSizes = partSizes; }

    public int getNextPartIndex() { return nextPartIndex; }
    public void setNextPartIndex(int nextPartIndex) { this.nextPartIndex = nextPartIndex; }

    public UploadStatus getStatus() { return status; }
    public void setStatus(UploadStatus status) { this.status = status; }

    public MergeMode getMergeMode() { return mergeMode; }
    public void setMergeMode(MergeMode mergeMode) { this.mergeMode = mergeMode; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
