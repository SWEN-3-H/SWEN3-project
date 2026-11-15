package at.fhtw.documentmanager.controller;

import at.fhtw.documentmanager.messaging.MessagingException;
import at.fhtw.documentmanager.search.SearchException;
import at.fhtw.documentmanager.service.DuplicateLabelException;
import at.fhtw.documentmanager.service.InvalidDocumentException;
import at.fhtw.documentmanager.service.ResourceNotFoundException;
import at.fhtw.documentmanager.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import at.fhtw.documentmanager.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException e){
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(404, e.getMessage()));
    }

    @ExceptionHandler({InvalidDocumentException.class, DuplicateLabelException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException e){
        log.error("Invalid request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
    }

    @ExceptionHandler(MessagingException.class)
    public ResponseEntity<ErrorResponse> messaging(MessagingException e){
        log.error("Messaging failure", e);
        return ResponseEntity.status(503).body(new ErrorResponse(503, e.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> storage(StorageException e) {
        log.error("Document storage failure", e);

        return ResponseEntity.status(503)
                .body(new ErrorResponse(
                        503,
                        "Document storage is currently unavailable."
                ));
    }

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ErrorResponse> search(SearchException e) {
        log.error("Search failure", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, "Search is currently unavailable"));
    }
}
