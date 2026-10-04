package it.polito.wa2.userdetailservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ProblemDetail handleUserException(UserException e) {
        HttpStatus status;
        switch (e.getErrorCode()) {
            case "USER_NOT_FOUND":
                status = HttpStatus.NOT_FOUND;
                break;
            case "EMAIL_ALREADY_EXISTS":
            case "USER_CONFLICT":
                status = HttpStatus.CONFLICT;
                break;
            case "USER_BAD_REQUEST":
            default:
                status = HttpStatus.BAD_REQUEST;
                break;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(URI.create("/problems/" + e.getErrorCode().toLowerCase()));

        String title = Arrays.stream(e.getErrorCode().replace("_", " ").split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
        problem.setTitle(title);
        problem.setProperty("errorCode", e.getErrorCode());

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("/problems/validation-failed"));

        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (msg1, msg2) -> msg1));
        
        problem.setProperty("errors", errors);
        return problem;
    }
}
