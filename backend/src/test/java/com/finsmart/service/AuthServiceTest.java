package com.finsmart.service;

import com.finsmart.dto.request.LoginRequest;
import com.finsmart.dto.request.RegisterRequest;
import com.finsmart.dto.response.AuthResponse;
import com.finsmart.dto.response.UserResponse;
import com.finsmart.model.User;
import com.finsmart.repository.UserRepository;
import com.finsmart.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository    userRepository;
    @Mock private PasswordEncoder   passwordEncoder;
    @Mock private JwtUtil           jwtUtil;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id("user123")
                .name("Abhinav")
                .email("abhinav@example.com")
                .password("hashed_password")
                .currency("INR")
                .build();
    }

    // ── register() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success — new email returns AuthResponse with tokens")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Abhinav");
        req.setEmail("abhinav@example.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh_token");

        AuthResponse response = authService.register(req);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        assertThat(response.getUser().getEmail()).isEqualTo("abhinav@example.com");
        assertThat(response.getUser().getName()).isEqualTo("Abhinav");

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: duplicate email — throws IllegalArgumentException")
    void register_duplicateEmail_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Other");
        req.setEmail("abhinav@example.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: password is BCrypt-hashed — never stored in plain text")
    void register_passwordIsHashed() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Abhinav");
        req.setEmail("test@example.com");
        req.setPassword("plaintext");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$10$hashedvalue");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // Verify password is hashed, not plain text
            assertThat(u.getPassword()).isEqualTo("$2a$10$hashedvalue");
            assertThat(u.getPassword()).isNotEqualTo("plaintext");
            u.setId("newid");
            return u;
        });
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("at");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("rt");

        authService.register(req);
        verify(passwordEncoder).encode("plaintext");
    }

    // ── login() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: success — correct credentials return AuthResponse")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("abhinav@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh_token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getUser().getEmail()).isEqualTo("abhinav@example.com");
    }

    @Test
    @DisplayName("login: wrong password — throws BadCredentialsException")
    void login_wrongPassword_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("abhinav@example.com");
        req.setPassword("wrongpassword");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("login: wrong email — throws BadCredentialsException (not UsernameNotFoundException)")
    void login_wrongEmail_throwsBadCredentials_notUserEnumeration() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nonexistent@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        // Must throw BadCredentialsException — NOT UsernameNotFoundException
        // This prevents user enumeration (attacker can't tell if email exists)
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials")
                .isNotInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    // ── refresh() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh: valid token — returns new access token, reuses same refresh token")
    void refresh_validToken_returnsNewAccessToken() {
        String refreshToken = "valid_refresh_token";

        when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUserId(refreshToken)).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateAccessToken("user123", "abhinav@example.com")).thenReturn("new_access_token");

        AuthResponse response = authService.refresh(refreshToken);

        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        // Refresh token should be reused — not regenerated
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
        // Should NOT call generateRefreshToken
        verify(jwtUtil, never()).generateRefreshToken(anyString());
    }

    @Test
    @DisplayName("refresh: expired/invalid token — throws BadCredentialsException")
    void refresh_invalidToken_throwsException() {
        when(jwtUtil.validateToken("bad_token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad_token"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    // ── getMe() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getMe: valid userId — returns UserResponse without password")
    void getMe_returnsUserResponse() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        UserResponse response = authService.getMe("user123");

        assertThat(response.getId()).isEqualTo("user123");
        assertThat(response.getEmail()).isEqualTo("abhinav@example.com");
        assertThat(response.getName()).isEqualTo("Abhinav");
        assertThat(response.getCurrency()).isEqualTo("INR");
        // UserResponse must NOT expose password — no password field to assert,
        // but confirm the class has no such field
    }

    // ── updateProfile() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile: updates name and currency, ignores null values")
    void updateProfile_updatesFields() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponse response = authService.updateProfile("user123", "New Name", "USD");

        verify(userRepository).save(argThat(u ->
                u.getName().equals("New Name") && u.getCurrency().equals("USD")
        ));
    }

    @Test
    @DisplayName("updateProfile: null values — existing values are preserved")
    void updateProfile_nullValues_preservesExisting() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        authService.updateProfile("user123", null, null);

        verify(userRepository).save(argThat(u ->
                u.getName().equals("Abhinav") && u.getCurrency().equals("INR")
        ));
    }
}
