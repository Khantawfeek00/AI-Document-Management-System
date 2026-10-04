package it.polito.wa2.fileservice.repository;

import it.polito.wa2.fileservice.entities.FileShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileShareRepository extends JpaRepository<FileShare, String> {
    List<FileShare> findByFile_Id(String fileId);
    List<FileShare> findBySharedWithUserId(String userId);
    boolean existsByFile_IdAndSharedWithUserId(String fileId, String userId);
}
