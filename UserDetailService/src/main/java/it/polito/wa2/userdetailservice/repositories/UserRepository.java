package it.polito.wa2.userdetailservice.repositories;

import it.polito.wa2.userdetailservice.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the User entity.
 * This interface automatically provides CRUD operations
 * (Create, Read, Update, Delete) for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    User findByEmail(String email);
    Page<User> findByDepartment(String department, Pageable pageable);
}
