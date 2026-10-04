package it.polito.wa2.fileservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polito.wa2.fileservice.dtos.*;
import it.polito.wa2.fileservice.entities.UploadStatus;
import it.polito.wa2.fileservice.entities.SharePermission;
import it.polito.wa2.fileservice.services.FileService;
import it.polito.wa2.fileservice.services.FileShareService;
import it.polito.wa2.fileservice.services.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "File API", description = "File API endpoints for managing uploaded files")
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;
    private final UploadService uploadService;
    private final FileShareService fileShareService;

    public FileController(FileService fileService, UploadService uploadService, FileShareService fileShareService) {
        this.fileService = fileService;
        this.uploadService = uploadService;
        this.fileShareService = fileShareService;
    }

    @Operation(summary = "GET /api/v1/files", description = "Retrieves a paginated list of uploaded files with optional filtering by status and content type.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK - List of files retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PagedFileResponse.class)
            )
        )
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<PagedFileResponse> listFiles(
            @RequestParam(value = "status", required = false) UploadStatus status,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "scope", required = false) String scope,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        long startTime = System.currentTimeMillis();
        var result = fileService.getFiles(status, contentType, scope, pageable);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=list_files status={} page={} size={} totalElements={} durationMs={}",
                status, pageable.getPageNumber(), result.getSize(), result.getTotalElements(), duration);

        return ResponseEntity.ok(new PagedFileResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        ));
    }

    @Operation(summary = "GET /api/v1/files/{id}", description = "Retrieves metadata of a specific file by its ID.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK - File metadata retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FileDetailDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - Upload with id not found"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<FileDetailDTO> getFile(@PathVariable("id") String id) {
        long startTime = System.currentTimeMillis();
        var file = fileService.getFileById(id);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=get_file_details fileId={} durationMs={}", id, duration);
        return ResponseEntity.ok(file);
    }

    @Operation(summary = "PATCH /api/v1/files/{id}", description = "Updates metadata of a specific file by its ID.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK - File metadata updated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FileDetailDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Not Found - Upload with id not found"
        )
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<FileDetailDTO> updateFile(
            @PathVariable("id") String id,
            @RequestBody UpdateFileRequest request) {
        long startTime = System.currentTimeMillis();
        var updated = fileService.updateFileMetadata(id, request);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=patch_file fileId={} durationMs={}", id, duration);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "DELETE /api/v1/files/{id}", description = "Deletes a specific file by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content - File deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<Void> deleteFile(@PathVariable("id") String id) {
        long startTime = System.currentTimeMillis();
        fileService.deleteFile(id);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=delete_file fileId={} durationMs={}", id, duration);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "GET /api/v1/files/{id}/download", description = "Downloads the file content for a specific file by its ID.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK - File downloaded successfully",
            content = @Content(mediaType = "application/octet-stream")
        ),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id not found")
    })
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management', 'ai-worker')")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable("id") String id) throws Exception {
        long startTime = System.currentTimeMillis();
        var streamResult = fileService.getFileStream(id);
        var upload = streamResult.getKey();
        var inputStream = streamResult.getValue();

        StreamingResponseBody streamingBody = outputStream -> {
            try (inputStream) {
                inputStream.transferTo(outputStream);
            }
        };

        String filename = upload.getFileId() != null ? fileService.getFilenameForFileId(upload.getFileId()) : "file";
        if (filename == null) filename = "file";
        
        String ct = upload.getFileId() != null ? fileService.getContentTypeForFileId(upload.getFileId()) : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (ct == null) ct = MediaType.APPLICATION_OCTET_STREAM_VALUE;

        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=download_file_start fileId={} filename={} size={} contentType={} durationMs={}",
                id, filename, upload.getUploadLength(), ct, duration);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, upload.getUploadLength() != null ? upload.getUploadLength().toString() : "0")
                .body(streamingBody);
    }

    @Operation(summary = "POST /api/v1/files/{fileId}/versions", description = "Create a new upload for a new version of an existing file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created - New upload for file version created successfully", content = {}),
        @ApiResponse(responseCode = "412", description = "Precondition Failed - Invalid Tus version"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid request headers"),
        @ApiResponse(responseCode = "404", description = "Not Found - File with id not found")
    })
    @PostMapping("/{fileId}/versions")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<Void> createFileVersion(
            @PathVariable("fileId") String fileId,
            @RequestHeader(value = "Tus-Resumable", required = false) String tusResumable,
            @RequestHeader(value = "Upload-Length", required = false) String uploadLengthHeader,
            @RequestHeader(value = "Upload-Metadata", required = false) String uploadMetadataHeader,
            @RequestHeader(value = "Upload-Checksum", required = false) String uploadChecksumHeader) {
        
        long startTime = System.currentTimeMillis();

        if (!BasicController.TUS_VERSION.equals(tusResumable)) {
            logger.warn("event=post_invalid_tus_version receivedVersion={}", tusResumable);
            throw new it.polito.wa2.fileservice.exception.InvalidTusVersionException(tusResumable, BasicController.TUS_VERSION);
        }

        Long uploadLength = null;
        if (uploadLengthHeader != null) {
            try {
                uploadLength = Long.parseLong(uploadLengthHeader);
            } catch (NumberFormatException e) {
                // Ignore, handled below
            }
        }
        
        if (uploadLength == null) {
            logger.warn("event=post_missing_upload_length_for_version fileId={}", fileId);
            throw new it.polito.wa2.fileservice.exception.UploadBadRequestException("Upload-Length header required");
        }

        var upload = uploadService.createUploadForFile(fileId, uploadLength, uploadMetadataHeader, uploadChecksumHeader);

        String location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .replacePath("/uploads/")
                .path("{id}")
                .buildAndExpand(upload.getId())
                .toUriString();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Location", location);
        responseHeaders.add("Tus-Resumable", BasicController.TUS_VERSION);
        responseHeaders.add("Upload-Expires", DateTimeFormatter.RFC_1123_DATE_TIME.format(upload.getExpiresAt().atZone(ZoneOffset.UTC)));

        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=post_version_upload_created fileId={} uploadId={} location={} durationMs={}", fileId, upload.getId(), location, duration);

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
    }

    @Operation(summary = "GET /api/v1/files/{id}/shares", description = "Retrieves all shares for a specific file")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK - List of file shares retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FileShareDTO.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id not found")
    })
    @GetMapping("/{id}/shares")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<List<FileShareDTO>> getFileShares(@PathVariable("id") String id) {
        var shares = fileShareService.getFileShares(id);
        return ResponseEntity.ok(shares);
    }

    @Operation(summary = "POST /api/v1/files/{id}/shares", description = "Create a new share for a file")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Created - File share created successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FileShareDTO.class)
            )
        ),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id not found"),
        @ApiResponse(responseCode = "400", description = "Bad Request - User does not exist or share already exists")
    })
    @PostMapping("/{id}/shares")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<FileShareDTO> createShare(
            @PathVariable("id") String id,
            @RequestParam("userId") String userId,
            @RequestParam("permission") SharePermission permission) {
        var share = fileShareService.createShare(id, userId, permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(share);
    }

    @Operation(summary = "GET /api/v1/files/shared-with/{userId}", description = "Get all files shared with a specific user")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK - List of files shared with user retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SharedFileDTO.class)
            )
        )
    })
    @GetMapping("/shared-with/{userId}")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<List<SharedFileDTO>> getFilesSharedWithUser(@PathVariable("userId") String userId) {
        var files = fileShareService.getFilesSharedWithUser(userId);
        return ResponseEntity.ok(files);
    }

    @Operation(summary = "DELETE /api/v1/files/{id}/shares/{shareId}", description = "Delete a file share")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content - File share deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Not Found - File share or file not found")
    })
    @DeleteMapping("/{id}/shares/{shareId}")
    @PreAuthorize("hasAnyRole('admin', 'manager', 'staff', 'user', 'default-roles-file-management')")
    public ResponseEntity<Void> deleteShare(
            @PathVariable("id") String id,
            @PathVariable("shareId") String shareId) {
        fileShareService.deleteShare(id, shareId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "POST /api/v1/files/{fileVersionId}/ai-metadata",
        description = "Store AI-generated metadata for a specific file version."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK - AI metadata saved successfully"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid payload"),
        @ApiResponse(responseCode = "404", description = "Not Found - File version not found")
    })
    @PostMapping("/{fileVersionId}/ai-metadata")
    @PreAuthorize("hasRole('ai-worker')")
    public ResponseEntity<Void> setAiMetadata(
            @PathVariable("fileVersionId") String fileVersionId,
            @RequestBody SetAiMetadataRequest request) {
        long startTime = System.currentTimeMillis();
        fileService.saveAiMetadata(fileVersionId, request);
        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=set_ai_metadata fileVersionId={} durationMs={}", fileVersionId, duration);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "POST /api/v1/files/query",
        description = "Query documents using RAG"
    )
    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('user', 'staff', 'manager', 'admin', 'default-roles-file-management')")
    public ResponseEntity<DocumentQueryResponse> queryDocuments(
            @RequestBody DocumentQueryRequest request) {
        var response = fileService.queryDocuments(request);
        return ResponseEntity.ok(response);
    }
}
