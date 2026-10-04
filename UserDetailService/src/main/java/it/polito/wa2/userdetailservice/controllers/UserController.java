package it.polito.wa2.userdetailservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polito.wa2.userdetailservice.dtos.CreateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UpdateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UserDTO;
import it.polito.wa2.userdetailservice.services.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "User API", description = "User API endpoints for managing user details")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/v1/users/me
     * Retrieves the details of the currently authenticated user.
     */
    @Operation(summary = "GET /api/v1/users/me", description = "Retrieves the details of the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - Authenticated user details retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDTO.class)
                    )
            )
    })
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID id = UUID.fromString(jwt.getSubject());
        return userService.getById(id);
    }

    /**
     * GET /api/v1/users/internal/{id}/department
     * Retrieves the department of a user by their ID.
     * Accessible only by service accounts.
     */
    @Operation(summary = "GET /api/v1/users/internal/{id}/department", description = "Retrieves the department of a user by their ID. Accessible only by service accounts.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - User department retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String.class)
                    )
            )
    })
    @GetMapping("/internal/{id}/department")
    @PreAuthorize("hasRole('serviceaccount-fileservice')")
    public String getUserDepartment(@PathVariable UUID id) {
        return userService.getDepartment(id);
    }

    /**
     * GET /api/v1/users/internal/department/{department}/ids
     * Retrieves the list of user IDs in a specific department.
     * Accessible only by service accounts.
     */
    @Operation(summary = "GET /api/v1/users/internal/department/{department}/ids", description = "Retrieves the list of user IDs in a specific department. Accessible only by service accounts.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - User IDs retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class))
                    )
            )
    })
    @GetMapping("/internal/department/{department}/ids")
    @PreAuthorize("hasRole('serviceaccount-fileservice')")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getUserIdsByDepartment(@PathVariable("department") String department) {
        return userService.getUserIdsByDepartment(department);
    }

    /**
     * GET /api/v1/users
     * Retrieves a list of all users.
     */
    @Operation(summary = "GET /api/v1/users", description = "Retrieves a paginated list of all users.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - List of users retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = UserDTO.class))
                    )
            )
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'serviceaccount-fileservice')")
    @ResponseStatus(HttpStatus.OK)
    public Page<UserDTO> getAllUsers(
            @RequestParam(value = "department", required = false) String department,
            @PageableDefault(size = 20, sort = {"fullName"}) Pageable pageable
    ) {
        if (department != null) {
            return userService.getAllByDepartment(department, pageable);
        } else {
            return userService.getAll(pageable);
        }
    }

    /**
     * GET /api/v1/users/{id}
     * Retrieves a specific user by their ID.
     */
    @Operation(summary = "GET /api/v1/users/{id}", description = "Retrieves a user by their unique ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - User retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User with id not found",
                    content = @Content()
            )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'serviceaccount-fileservice')")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO getUserById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    /**
     * POST /api/v1/users
     * Creates a new user.
     */
    @Operation(summary = "POST /api/v1/users", description = "Creates a new user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Created - User created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - User with email already exists",
                    content = @Content()
            )
    })
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(@Valid @RequestBody CreateUserDTO dto) {
        return userService.createUser(dto);
    }

    /**
     * PUT /api/v1/users/{id}
     * Updates an existing user's details.
     */
    @Operation(summary = "PUT /api/v1/users/{id}", description = "Update an existing user with the provided fields.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - User updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User with id not found",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - User with email already exists",
                    content = @Content()
            )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @ResponseStatus(HttpStatus.OK)
    public UserDTO updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserDTO dto
    ) {
        return userService.updateUser(id, dto);
    }

    /**
     * DELETE /api/v1/users/{id}
     * Removes a user.
     */
    @Operation(summary = "DELETE /api/v1/users/{id}", description = "Deletes a user by their unique ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "No Content - User deleted successfully",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User with id not found",
                    content = @Content()
            )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }

    /**
     * GET /api/v1/users/roles
     * Retrieves the list of all possible user roles.
     */
    @Operation(summary = "GET /api/v1/users/roles", description = "Retrieves the list of all possible user roles.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - List of user roles retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = String.class))
                    )
            )
    })
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('admin', 'serviceaccount-fileservice')")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getAllUserRoles() {
        return userService.getAllUserRoles();
    }
}
