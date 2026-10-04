package it.polito.wa2.fileservice.repository;

import it.polito.wa2.fileservice.entities.Upload;
import it.polito.wa2.fileservice.entities.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {
    Page<Upload> findByStatus(UploadStatus status, Pageable pageable);
    List<Upload> findByFileId(String fileId);

    @Query("SELECT u FROM Upload u WHERE u.expiresAt < :now AND u.status NOT IN ('DELETED', 'EXPIRED', 'COMPLETED')")
    List<Upload> findExpiredUploads(@Param("now") Instant now);
}
