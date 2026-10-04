package it.polito.wa2.fileservice.repository;

import it.polito.wa2.fileservice.entities.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, String> {
    long countByFile_Id(String fileId);
    Optional<FileVersion> findTopByFile_IdOrderByVersionNumberDesc(String fileId);
}
