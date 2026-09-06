package com.marketpulse.backend.config;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.marketpulse.backend.auth.EmailAlreadyRegisteredException;
import com.marketpulse.backend.auth.InvalidCredentialsException;
import com.marketpulse.backend.watchlist.DuplicateTickerException;
import com.marketpulse.backend.watchlist.WatchlistNotFoundException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ResponseEntity<Map<String, String>> duplicateEmail() { return error(HttpStatus.CONFLICT, "An account with that email already exists."); }
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Map<String, String>> invalidCredentials() { return error(HttpStatus.UNAUTHORIZED, "Invalid email or password."); }
    @ExceptionHandler(WatchlistNotFoundException.class)
    ResponseEntity<Map<String, String>> watchlistNotFound() { return error(HttpStatus.NOT_FOUND, "Watchlist not found."); }
    @ExceptionHandler(DuplicateTickerException.class)
    ResponseEntity<Map<String, String>> duplicateTicker() { return error(HttpStatus.CONFLICT, "That ticker is already in this watchlist."); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, message);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalidArgument(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<Map<String, String>> entityNotFound() { return error(HttpStatus.NOT_FOUND, "Resource not found."); }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("error", message)); }
}
