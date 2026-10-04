package it.polito.wa2.userdetailservice.services;

import it.polito.wa2.userdetailservice.UserMapper;
import it.polito.wa2.userdetailservice.dtos.CreateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UpdateUserDTO;
import it.polito.wa2.userdetailservice.dtos.UserDTO;
import it.polito.wa2.userdetailservice.entities.Role;
import it.polito.wa2.userdetailservice.entities.User;
import it.polito.wa2.userdetailservice.exceptions.EmailAlreadyExistsException;
import it.polito.wa2.userdetailservice.exceptions.UserException;
import it.polito.wa2.userdetailservice.exceptions.UserNotFoundException;
import it.polito.wa2.userdetailservice.keycloak.KeycloakClient;
import it.polito.wa2.userdetailservice.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KeycloakClient kc;

    public UserServiceImpl(UserRepository userRepository, KeycloakClient kc) {
        this.userRepository = userRepository;
        this.kc = kc;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllByDepartment(String department, Pageable pageable) {
        return userRepository.findByDepartment(department, pageable).map(UserMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserMapper.toDTO(user);
    }

    @Override
    public UserDTO createUser(CreateUserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException(dto.getEmail());
        }

        String fullName = dto.getFullName();
        String firstName = null;
        String lastName = null;

        if (fullName != null) {
            String[] parts = fullName.split(" ", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1].trim();
                if (lastName.isEmpty()) {
                    lastName = null;
                }
            }
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("organization", dto.getOrganization() != null ? dto.getOrganization() : "");

        String idStr = kc.createUser(dto.getEmail(), firstName, lastName, attributes, dto.getPassword());

        User user = UserMapper.toEntity(dto);
        user.setId(UUID.fromString(idStr));
        User savedUser = userRepository.save(user);
        return UserMapper.toDTO(savedUser);
    }

    @Override
    public UserDTO updateUser(UUID id, UpdateUserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            boolean exists = userRepository.existsByEmail(dto.getEmail());
            if (exists) {
                throw new EmailAlreadyExistsException(dto.getEmail());
            }
        }

        String kcId = kc.findUserIdByEmail(user.getEmail());
        if (kcId == null) {
            throw new UserException("User " + user.getId() + " has no Keycloak ID and cannot be updated in Keycloak", "BAD_REQUEST");
        }

        String fullName = dto.getFullName();
        String firstName = null;
        String lastName = null;

        if (fullName != null) {
            String[] parts = fullName.split(" ", 2);
            firstName = parts[0];
            if (parts.length > 1) {
                lastName = parts[1].trim();
                if (lastName.isEmpty()) {
                    lastName = null;
                }
            }
        }

        String newEmail = dto.getEmail() != null ? dto.getEmail() : user.getEmail();
        String newOrganization = dto.getOrganization() != null ? dto.getOrganization() : user.getOrganization();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("organization", newOrganization);

        kc.updateUser(kcId, newEmail, firstName, lastName, attributes);

        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            kc.setPassword(kcId, dto.getPassword());
        }

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getOrganization() != null) user.setOrganization(dto.getOrganization());
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getDepartment() != null) user.setDepartment(dto.getDepartment());
        if (dto.getStorageQuota() != null) user.setStorageQuota(dto.getStorageQuota());
        if (dto.getNotes() != null) user.setNotes(dto.getNotes());
        
        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        return UserMapper.toDTO(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        String kcId = kc.findUserIdByEmail(user.getEmail());
        if (kcId != null) {
            kc.deleteUser(kcId);
        }

        userRepository.deleteById(id);
    }

    @Override
    public String getDepartment(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return user.getDepartment();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getUserIdsByDepartment(String department) {
        return userRepository.findByDepartment(department, Pageable.unpaged())
                .getContent()
                .stream()
                .map(u -> u.getId().toString())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllUserRoles() {
        return Arrays.stream(Role.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
