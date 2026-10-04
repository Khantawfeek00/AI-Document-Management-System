package it.polito.wa2.aiprocessingservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        logger.error("Unhandled exception: " + e.getMessage(), e);

        String errorMessage;
        String msg = e.getMessage();
        if (msg != null && msg.contains("Connection refused")) {
            errorMessage = "Service dependency unavailable. Please ensure Ollama and PostgreSQL are running.";
        } else if (msg != null && (msg.contains("Ollama") || msg.contains("embedding"))) {
            errorMessage = "Embedding service unavailable. Please ensure Ollama is running with nomic-embed-text model.";
        } else if (msg != null && (msg.contains("GROQ") || msg.contains("chat"))) {
            errorMessage = "AI chat service unavailable. Please check GROQ API key configuration.";
        } else if (msg != null && (msg.contains("vector") || msg.contains("pgvector"))) {
            errorMessage = "Vector database unavailable. Please ensure PostgreSQL with pgvector is running.";
        } else {
            errorMessage = "An internal error occurred: " + msg;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("error", errorMessage);
        body.put("details", msg != null ? msg : "Unknown error");
        body.put("type", e.getClass().getSimpleName());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
