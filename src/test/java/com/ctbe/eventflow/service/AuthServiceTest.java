// src/test/java/com/ctbe/eventflow/service/AuthServiceTest.java
package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.LoginRequest;
import com.ctbe.eventflow.dto.request.RegisterRequest;
import com.ctbe.eventflow.dto.response.TokenResponse;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.ConflictException;
import com.ctbe.eventflow.mapper.UserMapper;
import com.ctbe.eventflow.model.TokenBlacklist;
import com.ctbe.eventflow.model.User;
import com.ctbe.eventflow.model.UserRole;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.repository.UserRepository;
import com.ctbe.eventflow.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtils jwtUtils;
    @Mock TokenBlacklistRepository tokenBlacklistRepository;
    @Mock UserMapper userMapper;

    @InjectMocks AuthService authService;

    private RegisterRequest registerRequest;
    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Alice");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("password123");

        user = User.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .passwordHash("hashed").role(UserRole.ATTENDEE).active(true)
                .build();

        userDTO = UserDTO.builder()
                .id(1L).name("Alice").email("alice@example.com")
                .role(UserRole.ATTENDEE).active(true)
                .build();
    }

    // ── register ──────────────────────────────────────────────

    @Test
    void register_happyPath_returnsUserDTO() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = authService.register(registerRequest);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_passwordIsEncoded() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void register_newUserHasAttendeeRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ATTENDEE);
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_duplicateEmail_doesNotSave() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest));
        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken("alice@example.com")).thenReturn("jwt.token.here");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        TokenResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(86400000L);
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void login_badCredentials_throwsException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── logout ────────────────────────────────────────────────

    @Test
    void logout_validToken_savesToBlacklist() {
        String token = "some.jwt.token";
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);
        when(jwtUtils.getExpiryFromToken(token)).thenReturn(expiry);

        authService.logout(token);

        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(token);
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiry);
    }
}