package com.marketpulse.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.marketpulse.backend.auth.AuthDtos.AuthResponse;
import com.marketpulse.backend.auth.AuthDtos.LoginRequest;
import com.marketpulse.backend.auth.AuthDtos.RegisterRequest;
import com.marketpulse.backend.auth.AuthDtos.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { 
        return authService.register(request); 
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { 
        return authService.login(request); 
    }

    @GetMapping("/me")
    public UserResponse me(org.springframework.security.core.Authentication authentication) {
        return authService.current(authentication.getName());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() { }
}
