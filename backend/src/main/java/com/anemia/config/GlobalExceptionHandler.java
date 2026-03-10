package com.anemia.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handleStatus(ResponseStatusException ex) { return build(ex.getStatusCode().value(), ex.getReason()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        return build(400, ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage()).collect(Collectors.joining(", ")));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleAll(Exception ex) { return build(500, "Unexpected error occurred."); }
    private ResponseEntity<Map<String,Object>> build(int status, String msg) {
        return ResponseEntity.status(status).body(Map.of("status",status,"message",msg,"timestamp",LocalDateTime.now().toString()));
    }
}