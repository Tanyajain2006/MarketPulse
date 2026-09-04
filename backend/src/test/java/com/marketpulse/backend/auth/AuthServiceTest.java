package com.marketpulse.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.marketpulse.backend.auth.AuthDtos.AuthResponse;
import com.marketpulse.backend.auth.AuthDtos.LoginRequest;
import com.marketpulse.backend.auth.AuthDtos.RegisterRequest;
import com.marketpulse.backend.user.User;
import com.marketpulse.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository users;
    @Mock private JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService service;

    @BeforeEach
    void setUp() { service = new AuthService(users, passwordEncoder, jwtService); }

    @Test
    void registerHashesPasswordAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Ada Lovelace", " ADA@Example.com ", "correct horse");
        when(users.existsByEmail("ada@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("ada@example.com")).thenReturn("jwt");

        AuthResponse response = service.register(request);

        assertThat(response.token()).isEqualTo("jwt");
        assertThat(response.user().email()).isEqualTo("ada@example.com");
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(users).save(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash()).isNotEqualTo("correct horse");
        assertThat(passwordEncoder.matches("correct horse", savedUser.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ada", "ada@example.com", "correct horse")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(users, never()).save(any(User.class));
    }

    @Test
    void loginRequiresCorrectPassword() {
        User user = new User("Ada", "ada@example.com", passwordEncoder.encode("correct horse"));
        when(users.findByEmail("ada@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken("ada@example.com")).thenReturn("jwt");

        assertThat(service.login(new LoginRequest("ada@example.com", "correct horse")).token()).isEqualTo("jwt");
        assertThatThrownBy(() -> service.login(new LoginRequest("ada@example.com", "wrong password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
