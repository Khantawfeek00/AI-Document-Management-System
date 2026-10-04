package it.polito.wa2.fileservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@RestControllerAdvice
public class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(ProblemDetailsHandler.class);

    private ProblemDetail createProblemDetail(HttpStatus status, String detail, String errorCode, String uploadId) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        try {
            problemDetail.setType(new URI("https://api.example.com/errors/" + errorCode));
        } catch (Exception ignored) {}
        problemDetail.setTitle(errorCode);
        if (uploadId != null) {
            problemDetail.setProperty("uploadId", uploadId);
        }
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        return problemDetail;
    }

    private ProblemDetail createProblemDetail(HttpStatus status, String detail, String errorCode) {
        return createProblemDetail(status, detail, errorCode, null);
    }

    @ExceptionHandler(UploadNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUploadNotFound(UploadNotFoundException e) {
        logger.warn("event=upload_not_found uploadId={} message={}", e.getUploadId(), e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                e.getMessage(),
                e.getErrorCode(),
                e.getUploadId()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(InvalidOffsetException.class)
    public ResponseEntity<ProblemDetail> handleInvalidOffset(InvalidOffsetException e) {
        logger.warn("event=invalid_offset expected={} received={} message={}", e.getExpected(), e.getReceived(), e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                e.getMessage(),
                e.getErrorCode()
        );
        problemDetail.setProperty("expected", e.getExpected());
        problemDetail.setProperty("received", e.getReceived());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(InvalidTusVersionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTusVersion(InvalidTusVersionException e) {
        logger.warn("event=invalid_tus_version received={} supported={}", e.getReceivedVersion(), e.getSupportedVersion());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.PRECONDITION_FAILED,
                e.getMessage(),
                e.getErrorCode()
        );
        HttpHeaders headers = new HttpHeaders();
        // basic controller version
        headers.add("Tus-Version", "1.0.0");
        headers.add("Tus-Resumable", "1.0.0");
        return new ResponseEntity<>(problemDetail, headers, HttpStatus.PRECONDITION_FAILED);
    }

    @ExceptionHandler(UnsupportedTusMediaTypeException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(UnsupportedTusMediaTypeException e) {
        logger.warn("event=unsupported_media_type receivedType={}", e.getReceivedType());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                e.getMessage(),
                e.getErrorCode()
        );
        problemDetail.setProperty("receivedContentType", e.getReceivedType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    @ExceptionHandler(UploadGoneException.class)
    public ResponseEntity<ProblemDetail> handleUploadGone(UploadGoneException e) {
        logger.warn("event=upload_gone uploadId={} message={}", e.getUploadId(), e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.GONE,
                e.getMessage(),
                e.getErrorCode(),
                e.getUploadId()
        );
        return ResponseEntity.status(HttpStatus.GONE).body(problemDetail);
    }

    @ExceptionHandler(UploadLengthExceededException.class)
    public ResponseEntity<ProblemDetail> handleUploadLengthExceeded(UploadLengthExceededException e) {
        logger.warn("event=upload_length_exceeded provided={} maxAllowed={}", e.getProvided(), e.getMaxAllowed());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                e.getMessage(),
                e.getErrorCode()
        );
        problemDetail.setProperty("provided", e.getProvided());
        problemDetail.setProperty("maxAllowed", e.getMaxAllowed());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problemDetail);
    }

    @ExceptionHandler(UploadBadRequestException.class)
    public ResponseEntity<ProblemDetail> handleUploadBadRequest(UploadBadRequestException e) {
        logger.warn("event=upload_bad_request reason={}", e.getReason());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e.getErrorCode()
        );
        if (e.getReason() != null && !e.getReason().isEmpty()) {
            problemDetail.setProperty("reason", e.getReason());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(UploadConflictException.class)
    public ResponseEntity<ProblemDetail> handleUploadConflict(UploadConflictException e) {
        logger.warn("event=upload_conflict reason={}", e.getReason());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                e.getMessage(),
                e.getErrorCode()
        );
        problemDetail.setProperty("reason", e.getReason());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(UploadStorageException.class)
    public ResponseEntity<ProblemDetail> handleUploadStorage(UploadStorageException e) {
        logger.error("event=storage_unavailable reason={} cause={}", e.getReason(), e.getStorageCause() != null ? e.getStorageCause().getMessage() : null);
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                e.getMessage(),
                e.getErrorCode()
        );
        problemDetail.setProperty("reason", e.getReason());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }

    @ExceptionHandler(UploadOffsetMismatchException.class)
    public ResponseEntity<ProblemDetail> handleUploadOffsetMismatch(UploadOffsetMismatchException e) {
        logger.warn("event=offset_mismatch uploadId={} expected={} received={}", e.getUploadId(), e.getExpected(), e.getReceived());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                e.getMessage(),
                e.getErrorCode(),
                e.getUploadId()
        );
        problemDetail.setProperty("expected", e.getExpected());
        problemDetail.setProperty("received", e.getReceived());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(UploadAlreadyCompletedException.class)
    public ResponseEntity<ProblemDetail> handleUploadAlreadyCompleted(UploadAlreadyCompletedException e) {
        logger.warn("event=upload_already_completed uploadId={}", e.getUploadId());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.CONFLICT,
                e.getMessage(),
                e.getErrorCode(),
                e.getUploadId()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(ChecksumMismatchException.class)
    public ResponseEntity<ProblemDetail> handleChecksumMismatch(ChecksumMismatchException e) {
        logger.error("event=checksum_mismatch uploadId={} expected={} actual={}", e.getUploadId(), e.getExpected(), e.getActual());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e.getErrorCode(),
                e.getUploadId()
        );
        problemDetail.setProperty("expected", e.getExpected());
        problemDetail.setProperty("actual", e.getActual());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception e) {
        logger.error("event=unexpected_error message={} stackTrace={}", e.getMessage(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "");
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        try {
            problemDetail.setType(new URI("https://api.example.com/errors/INTERNAL_SERVER_ERROR"));
        } catch (Exception ignored) {}
        problemDetail.setTitle("INTERNAL_SERVER_ERROR");
        problemDetail.setProperty("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleFileNotFound(FileNotFoundException e) {
        logger.warn("event=file_not_found fileId={} message={}", e.getFileId(), e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                e.getMessage(),
                e.getErrorCode()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(FileShareException.class)
    public ResponseEntity<ProblemDetail> handleFileShareException(FileShareException e) {
        logger.warn("event=file_share_error reason={} message={}", e.getReason(), e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                e.getErrorCode()
        );
        problemDetail.setProperty("reason", e.getReason());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("event=access_denied message={}", e.getMessage());
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.FORBIDDEN,
                e.getMessage() != null ? e.getMessage() : "Access denied",
                "ACCESS_DENIED"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }
}
