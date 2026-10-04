package it.polito.wa2.fileservice.clients;

import java.util.List;

public record PagedUserResponse(
    List<UserResponseDTO> content,
    Long totalElements,
    Integer totalPages,
    Integer size,
    Integer number
) {}
