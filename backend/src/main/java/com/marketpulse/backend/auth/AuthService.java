package com.marketpulse.backend.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.marketpulse.backend.auth.AuthDtos.AuthResponse;
import com.marketpulse.backend.auth.AuthDtos.LoginRequest;
import com.marketpulse.backend.auth.AuthDtos.RegisterRequest;
import com.marketpulse.backend.auth.AuthDtos.UserResponse;
import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) throw new EmailAlreadyRegisteredException();
        User user = users.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return response(user);
    }

    @Transactional(readOnly = true)
    public UserResponse current(String email) {
        return users.findByEmail(normalize(email)).map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwtService.generateToken(user.getEmail()),
                new UserResponse(user.getId(), user.getName(), user.getEmail()));
    }

    private String normalize(String email) { return email.trim().toLowerCase(); }
}
