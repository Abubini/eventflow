package com.ctbe.eventflow.exception;
import com.ctbe.eventflow.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) { return build(HttpStatus.NOT_FOUND, ex.getMessage(), req); }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) { return build(HttpStatus.CONFLICT, ex.getMessage(), req); }
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) { return build(HttpStatus.FORBIDDEN, ex.getMessage(), req); }
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest req) { return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) { return build(HttpStatus.FORBIDDEN, "Access denied", req); }
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) { return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String,String> fe = new HashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) fe.put(e.getField(), e.getDefaultMessage());
        ErrorResponse body = ErrorResponse.builder().status(400).error("Validation Failed").message("Input validation failed")
            .path(req.getRequestURI()).timestamp(LocalDateTime.now()).fieldErrors(fe).build();
        return ResponseEntity.badRequest().body(body);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) { return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req); }
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.builder().status(status.value())
            .error(status.getReasonPhrase()).message(message).path(req.getRequestURI()).timestamp(LocalDateTime.now()).build());
    }
}
