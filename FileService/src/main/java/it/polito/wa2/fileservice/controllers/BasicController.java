package it.polito.wa2.fileservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.polito.wa2.fileservice.entities.UploadStatus;
import it.polito.wa2.fileservice.services.UploadService;
import it.polito.wa2.fileservice.exception.*;
import it.polito.wa2.fileservice.services.ChecksumService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Tag(name = "Basic Upload API", description = "Endpoints for handling TUS protocol uploads")
@RestController
@CrossOrigin
public class BasicController {

    public static final String TUS_VERSION = "1.0.0";
    private static final long TUS_MAX_SIZE = 1073741824L; // 1GB

    private final Logger logger = LoggerFactory.getLogger(BasicController.class);

    private final UploadService uploadService;
    private final ChecksumService checksumService;

    public BasicController(UploadService uploadService, ChecksumService checksumService) {
        this.uploadService = uploadService;
        this.checksumService = checksumService;
    }

    @Operation(summary = "OPTIONS /uploads", description = "Handles OPTIONS requests for TUS uploads")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content - Successful OPTIONS request")
    })
    @RequestMapping(value = "/uploads", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> optionsUploads(@RequestHeader Map<String, String> headers) {
        long startTime = System.currentTimeMillis();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Tus-Version", TUS_VERSION);
        responseHeaders.add("Tus-Extension", "creation, termination, expiration, checksum");
        responseHeaders.add("Tus-Checksum-Algorithm", "sha256");
        responseHeaders.add("Tus-Max-Size", String.valueOf(TUS_MAX_SIZE));
        responseHeaders.add("Vary", "Origin");
        responseHeaders.add("Access-Control-Allow-Methods", "POST,HEAD,PATCH,OPTIONS,DELETE");
        responseHeaders.add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Final-Length, Upload-Metadata, Upload-Length, Tus-Resumable, Upload-Offset, Upload-Expires, Upload-Checksum");
        responseHeaders.add("Access-Control-Expose-Headers", "Tus-Version,Tus-Extension,Tus-Max-Size,Location,Upload-Offset,Upload-Length,Upload-Metadata,Upload-Expires, Tus-Checksum-Algorithm");

        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=options_request durationMs={}", duration);
        return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "HEAD /uploads/{id}", description = "Retrieves the status of an upload")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content - Upload exists and status retrieved successfully"),
        @ApiResponse(responseCode = "410", description = "Gone - has been removed or is no longer available"),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id ID not found")
    })
    @RequestMapping(value = "/uploads/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headUpload(@PathVariable("id") String id) {
        long startTime = System.currentTimeMillis();
        var upload = uploadService.getUpload(id);

        if (upload != null && (upload.getStatus() == UploadStatus.DELETED || upload.getExpiresAt().isBefore(Instant.now()))) {
            long duration = System.currentTimeMillis() - startTime;
            logger.warn("event=head_upload_gone uploadId={} status={} durationMs={}", id, upload.getStatus(), duration);
            throw new UploadGoneException(id);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Tus-Resumable", TUS_VERSION);
        responseHeaders.add("Upload-Offset", upload != null ? String.valueOf(upload.getCurrentOffset()) : "null");
        if (upload != null && upload.getUploadLength() != null) {
            responseHeaders.add("Upload-Length", String.valueOf(upload.getUploadLength()));
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=head_upload_success uploadId={} currentOffset={} uploadLength={} status={} durationMs={}", 
            id, upload != null ? upload.getCurrentOffset() : null, upload != null ? upload.getUploadLength() : null, upload != null ? upload.getStatus() : null, duration);
        
        return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "POST /uploads", description = "Creates a new upload")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created - Upload created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Bad request for upload operation op"),
        @ApiResponse(responseCode = "413", description = "Payload Too Large - Upload length provided exceeded the maximum allowed size"),
        @ApiResponse(responseCode = "415", description = "Unsupported Media Type - Invalid Tus-Resumable header: received X, supported Y")
    })
    @PostMapping("/uploads/")
    public ResponseEntity<Void> postUploads(
            @RequestHeader(value = "Tus-Resumable", required = false) String tusResumable,
            @RequestHeader(value = "Upload-Length", required = false) String uploadLengthHeader,
            @RequestHeader(value = "Upload-Metadata", required = false) String uploadMetadataHeader,
            @RequestHeader(value = "Upload-Checksum", required = false) String uploadChecksumHeader) {
        
        long startTime = System.currentTimeMillis();

        if (!TUS_VERSION.equals(tusResumable)) {
            logger.warn("event=post_invalid_tus_version receivedVersion={}", tusResumable);
            throw new InvalidTusVersionException(tusResumable, TUS_VERSION);
        }

        Long uploadLength = null;
        if (uploadLengthHeader != null) {
            try {
                uploadLength = Long.parseLong(uploadLengthHeader);
            } catch (NumberFormatException e) {
                // Ignore, will throw below
            }
        }
        
        if (uploadLength == null) {
            logger.warn("event=post_missing_upload_length");
            throw new UploadBadRequestException("Upload-Length header required");
        }

        if (uploadLength > TUS_MAX_SIZE) {
            logger.warn("event=post_length_exceeded provided={} maxAllowed={}", uploadLength, TUS_MAX_SIZE);
            throw new UploadLengthExceededException(uploadLength, TUS_MAX_SIZE);
        }

        var upload = uploadService.createUpload(uploadLength, uploadMetadataHeader, uploadChecksumHeader);
        String location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("{id}")
                .buildAndExpand(upload.getId())
                .toUriString();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Location", location);
        responseHeaders.add("Tus-Resumable", TUS_VERSION);
        String expiresStr = DateTimeFormatter.RFC_1123_DATE_TIME.format(upload.getExpiresAt().atZone(ZoneOffset.UTC));
        responseHeaders.add("Upload-Expires", expiresStr);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("event=post_upload_created uploadId={} location={} uploadLength={} checksumProvided={} durationMs={}",
                upload.getId(), location, uploadLength, uploadChecksumHeader != null && !uploadChecksumHeader.isBlank(), duration);

        return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);
    }

    @Operation(summary = "DELETE /uploads/{id}", description = "Deletes an existing upload")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content - Upload deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id ID not found"),
        @ApiResponse(responseCode = "415", description = "Unsupported Media Type - Invalid Tus-Resumable header: received X, supported Y"),
        @ApiResponse(responseCode = "503", description = "Service Unavailable - A storage error occurred during upload operation: x")
    })
    @DeleteMapping("/uploads/{id}")
    public ResponseEntity<Void> deleteUpload(
            @PathVariable("id") String id,
            @RequestHeader(value = "Tus-Resumable", required = false) String tusResumable) {
        
        long startTime = System.currentTimeMillis();

        if (!TUS_VERSION.equals(tusResumable)) {
            logger.warn("event=delete_invalid_tus_version uploadId={} receivedVersion={}", id, tusResumable);
            throw new InvalidTusVersionException(tusResumable, TUS_VERSION);
        }

        uploadService.deleteUpload(id);

        long duration = System.currentTimeMillis() - startTime;
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Tus-Resumable", TUS_VERSION);

        logger.info("event=delete_upload_success uploadId={} durationMs={}", id, duration);

        return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "PATCH /uploads/{id}", description = "Uploads a chunk to an existing upload")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "No Content - Chunk uploaded successfully"),
        @ApiResponse(responseCode = "400", description = "Bad Request - Bad request for upload operation op"),
        @ApiResponse(responseCode = "404", description = "Not Found - Upload with id ID not found"),
        @ApiResponse(responseCode = "409", description = "Conflict - Conflict in upload operation: x"),
        @ApiResponse(responseCode = "409", description = "Conflict - Invalid upload offset: expected x, received y"),
        @ApiResponse(responseCode = "409", description = "Conflict - Offset mismatch for upload id: expected x, received y"),
        @ApiResponse(responseCode = "409", description = "Conflict - Upload id already completed"),
        @ApiResponse(responseCode = "410", description = "Gone - Upload has been removed or is no longer available"),
        @ApiResponse(responseCode = "412", description = "Precondition Failed - Invalid Tus-Resumable header: received X, supported Y"),
        @ApiResponse(responseCode = "415", description = "Unsupported Media Type - Unsupported Content-Type for TUS patch: X"),
        @ApiResponse(responseCode = "503", description = "Service Unavailable - A storage error occurred during upload operation: x")
    })
    @PatchMapping("/uploads/{id}")
    public ResponseEntity<Void> patchUpload(
            @PathVariable("id") String id,
            @RequestHeader("Tus-Resumable") String tusVersion,
            @RequestHeader("Upload-Offset") Long uploadOffset,
            @RequestHeader("Content-Type") String contentType,
            @RequestHeader(value = "Upload-Checksum", required = false) String uploadChecksumHeader,
            @RequestBody(required = false) byte[] chunk) throws Exception {

        long startTime = System.currentTimeMillis();

        if (!TUS_VERSION.equals(tusVersion)) {
            logger.warn("event=patch_invalid_tus_version uploadId={} receivedVersion={}", id, tusVersion);
            throw new InvalidTusVersionException(tusVersion, TUS_VERSION);
        }
        if (contentType == null || !contentType.startsWith("application/offset+octet-stream")) {
            logger.warn("event=patch_invalid_content_type uploadId={} receivedType={}", id, contentType);
            throw new UnsupportedTusMediaTypeException(contentType);
        }
        if (uploadOffset == null) {
            logger.warn("event=patch_missing_upload_offset uploadId={}", id);
            throw new InvalidOffsetException(null, null);
        }
        if (chunk == null) {
            logger.warn("event=patch_missing_chunk uploadId={}", id);
            throw new UploadBadRequestException("Chunk data required");
        }

        if (uploadChecksumHeader != null) {
            logger.debug("event=patch_checksum_validation uploadId={} checksumHeader={}", id, uploadChecksumHeader);
            var parsed = checksumService.parseChecksumHeader(uploadChecksumHeader);
            String algorithm = parsed.getKey();
            String expectedChecksum = parsed.getValue();

            String actualChecksum = checksumService.calculateChunkChecksum(chunk, algorithm);

            if (!actualChecksum.equals(expectedChecksum)) {
                logger.error("event=patch_checksum_mismatch uploadId={} algorithm={} expected={} actual={}",
                        id, algorithm, expectedChecksum, actualChecksum);
                throw new ChecksumMismatchException(id, uploadChecksumHeader, actualChecksum);
            }
            logger.debug("event=patch_checksum_verified uploadId={} algorithm={}", id, algorithm);
        }

        Long updatedUpload = uploadService.patchUpload(
                id,
                uploadOffset,
                new ByteArrayInputStream(chunk),
                (long) chunk.length
        );

        long duration = System.currentTimeMillis() - startTime;
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Upload-Offset", updatedUpload.toString());
        responseHeaders.add("Tus-Resumable", TUS_VERSION);

        logger.info("event=patch_upload_success uploadId={} uploadOffset={} chunkSize={} totalParts={} durationMs={}",
                id, updatedUpload, chunk.length, uploadOffset / chunk.length + 1, duration);

        return new ResponseEntity<>(responseHeaders, HttpStatus.NO_CONTENT);
    }
}
