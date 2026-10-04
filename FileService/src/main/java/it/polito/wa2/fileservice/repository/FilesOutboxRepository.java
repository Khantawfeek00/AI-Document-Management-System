package it.polito.wa2.fileservice.repository;

import it.polito.wa2.fileservice.entities.FilesOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesOutboxRepository extends JpaRepository<FilesOutboxEvent, UUID> {
}
