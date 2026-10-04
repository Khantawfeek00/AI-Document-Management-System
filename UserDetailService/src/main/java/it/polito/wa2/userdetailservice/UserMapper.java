package it.polito.wa2.userdetailservice;

import it.polito.wa2.userdetailservice.dtos.CreateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UserDTO;
import it.polito.wa2.userdetailservice.entities.User;

import java.time.Instant;
import java.util.UUID;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id is null");
        }
        return new UserDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getOrganization(),
                user.getRole(),
                user.getDepartment(),
                user.getStorageQuota(),
                user.getNotes(),
                user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now(),
                user.getUpdatedAt() != null ? user.getUpdatedAt() : Instant.now()
        );
    }

    public static User toEntity(CreateUserDTO dto) {
        UUID id = null;
        if (dto.getId() != null) {
            try {
                id = UUID.fromString(dto.getId());
            } catch (IllegalArgumentException e) {
                // ignore or handle if needed
            }
        }
        return new User(
                id,
                dto.getFullName(),
                dto.getEmail(),
                dto.getOrganization(),
                dto.getRole(),
                dto.getDepartment(),
                dto.getStorageQuota(),
                dto.getNotes()
        );
    }
}
