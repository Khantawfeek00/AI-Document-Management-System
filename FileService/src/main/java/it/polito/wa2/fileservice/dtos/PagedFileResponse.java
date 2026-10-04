package it.polito.wa2.fileservice.dtos;

import java.util.List;

public record PagedFileResponse(
    List<FileSummaryDTO> content,
    Integer page,
    Integer size,
    Long totalElements,
    Integer totalPages
) {}
