package it.polito.wa2.userdetailservice.services;

import it.polito.wa2.userdetailservice.dtos.CreateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UpdateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service layer interface for User operations.
 * Defines the business logic for managing users.
 */
public interface UserService {
    
    /**
     * Retrieves a paginated list of all users.
     * @return a page of UserDTOs.
     */
    Page<UserDTO> getAll(Pageable pageable);

    /**
     * Retrieves a paginated list of users filtered by department.
     * @param department The department to filter users by.
     * @return a page of UserDTOs belonging to the specified department.
     */
    Page<UserDTO> getAllByDepartment(String department, Pageable pageable);

    /**
     * Retrieves a specific user by their ID.
     * @param id The UUID of the user to find.
     * @return The UserDTO if found, otherwise throws an exception.
     */
    UserDTO getById(UUID id);

    /**
     * Creates a new user based on the provided data.
     * @param dto The DTO containing new user details.
     * @return The DTO of the newly created user.
     */
    UserDTO createUser(CreateUserDTO dto);

    /**
     * Updates an existing user's details.
     * @param id The UUID of the user to update.
     * @param dto The DTO containing the fields to update.
     * @return The DTO of the updated user.
     */
    UserDTO updateUser(UUID id, UpdateUserDTO dto);

    /**
     * Deletes a user by their ID.
     * @param id The UUID of the user to delete.
     */
    void deleteUser(UUID id);

    String getDepartment(UUID id);

    /**
     * Retrieves a list of user IDs in a specific department.
     * @param department The department to filter users by.
     * @return A list of user IDs (as strings) belonging to the specified department.
     */
    List<String> getUserIdsByDepartment(String department);

    List<String> getAllUserRoles();
}
