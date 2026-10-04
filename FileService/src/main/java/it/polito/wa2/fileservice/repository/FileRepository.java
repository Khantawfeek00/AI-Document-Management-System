package it.polito.wa2.fileservice.repository;

import it.polito.wa2.fileservice.entities.File;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    Page<File> findByLatestContentType(String contentType, Pageable pageable);

    Page<File> findAllByOwnerIdIn(List<String> ownerIds, Pageable pageable);

    @Query("SELECT f.ownerId FROM File f WHERE f.id = :id")
    Optional<String> findOwnerIdByFileId(@Param("id") String id);

    @Query("SELECT DISTINCT f FROM File f LEFT JOIN f.shares s WHERE f.ownerId = :userId OR s.sharedWithUserId = :userId")
    Page<File> findAllVisibleForStaff(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT DISTINCT f FROM File f INNER JOIN f.shares s WHERE s.sharedWithUserId = :userId AND f.ownerId <> :userId")
    Page<File> findAllSharedWithUser(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT DISTINCT f FROM File f LEFT JOIN f.shares s WHERE f.ownerId IN :departmentUserIds OR f.ownerId = :currentUserId OR s.sharedWithUserId = :currentUserId")
    Page<File> findAllVisibleForManager(
        @Param("departmentUserIds") List<String> departmentUserIds,
        @Param("currentUserId") String currentUserId,
        Pageable pageable
    );
}
