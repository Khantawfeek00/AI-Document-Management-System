package it.polito.wa2.fileservice.dtos;

import it.polito.wa2.fileservice.entities.SharePermission;

public record CreateShareRequest(
    String userId,
    SharePermission permission
) {}
