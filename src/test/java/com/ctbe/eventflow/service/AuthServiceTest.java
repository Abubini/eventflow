package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.LoginRequest;
import com.ctbe.eventflow.dto.request.RegisterRequest;
import com.ctbe.eventflow.dto.response.TokenResponse;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.ConflictException;
import com.ctbe.eventflow.mapper.UserMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import com.ctbe.eventflow.security.JwtUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository            userRepository;
    @Mock PasswordEncoder           passwordEncoder;
    @Mock AuthenticationManager     authenticationManager;
    @Mock JwtUtils                  jwtUtils;
    @Mock TokenBlacklistRepository  tokenBlacklistRepository;
    @Mock UserMapper                userMapper;

    @InjectMocks AuthService authService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Alice").email("alice@test.com")
                .passwordHash("$2a$hashed").role(UserRole.ATTENDEE).active(true).build();
        userDTO = UserDTO.builder().id(1L).name("Alice").email("alice@test.com")
                .role(UserRole.ATTENDEE).active(true).build();
    }

    // ════════════════════════════════════════════════════
    //  register
    // ════════════════════════════════════════════════════

    @Test
    void register_newEmail_savesAndReturnsDTO() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = authService.register(makeRegisterRequest("Alice", "alice@test.com", "password123"));

        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsConflictException() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                makeRegisterRequest("Alice", "alice@test.com", "password123")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_duplicateEmail_neverCallsSave() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                makeRegisterRequest("Alice", "alice@test.com", "password123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_passwordIsHashed() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("mypassword")).thenReturn("$2a$hashed_pw");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        authService.register(makeRegisterRequest("Alice", "alice@test.com", "mypassword"));

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getPasswordHash()).isEqualTo("$2a$hashed_pw");
        // raw password must NEVER be stored
        assertThat(cap.getValue().getPasswordHash()).doesNotContain("mypassword");
    }

    @Test
    void register_newUserHasAttendeeRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        authService.register(makeRegisterRequest("Alice", "alice@test.com", "password123"));

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getRole()).isEqualTo(UserRole.ATTENDEE);
    }

    @Test
    void register_newUserIsActiveByDefault() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        authService.register(makeRegisterRequest("Alice", "alice@test.com", "password123"));

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().isActive()).isTrue();
    }

    @Test
    void register_nameIsPreservedExactly() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        authService.register(makeRegisterRequest("Alice Wonderland", "alice@test.com", "password123"));

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        assertThat(cap.getValue().getName()).isEqualTo("Alice Wonderland");
    }

    @Test
    void register_emailCaseSensitivityChecked() {
        when(userRepository.existsByEmail("Alice@Test.COM")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        assertThatCode(() -> authService.register(
                makeRegisterRequest("Alice", "Alice@Test.COM", "password123")))
                .doesNotThrowAnyException();
        // the exact email string passed in should be checked
        verify(userRepository).existsByEmail("Alice@Test.COM");
    }

    // ════════════════════════════════════════════════════
    //  login
    // ════════════════════════════════════════════════════

    @Test
    void login_correctCredentials_returnsTokenResponse() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken("alice@test.com")).thenReturn("jwt.token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        TokenResponse resp = authService.login(makeLoginRequest("alice@test.com", "password123"));

        assertThat(resp.getToken()).isEqualTo("jwt.token");
        assertThat(resp.getType()).isEqualTo("Bearer");
        assertThat(resp.getExpiresIn()).isEqualTo(86400000L);
        assertThat(resp.getUser().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(makeLoginRequest("alice@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_wrongPassword_neverGeneratesToken() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(makeLoginRequest("alice@test.com", "wrong")));
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    void login_unknownEmail_throwsBadCredentialsException() {
        doThrow(new BadCredentialsException("User not found"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(makeLoginRequest("ghost@test.com", "pass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_disabledAccount_throwsDisabledException() {
        doThrow(new DisabledException("Account disabled"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(makeLoginRequest("alice@test.com", "password123")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    void login_tokenTypeIsBearer() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(anyString())).thenReturn("token");
        when(jwtUtils.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        TokenResponse resp = authService.login(makeLoginRequest("alice@test.com", "password123"));
        assertThat(resp.getType()).isEqualTo("Bearer");
    }

    @Test
    void login_expiresInMatchesJwtUtils() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken(anyString())).thenReturn("token");
        when(jwtUtils.getExpirationMs()).thenReturn(3600000L); // 1 hour
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        TokenResponse resp = authService.login(makeLoginRequest("alice@test.com", "password123"));
        assertThat(resp.getExpiresIn()).isEqualTo(3600000L);
    }

    // ════════════════════════════════════════════════════
    //  logout
    // ════════════════════════════════════════════════════

    @Test
    void logout_validToken_savesToBlacklist() {
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);
        when(jwtUtils.getExpiryFromToken("valid.jwt.token")).thenReturn(expiry);

        authService.logout("valid.jwt.token");

        ArgumentCaptor<TokenBlacklist> cap = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(cap.capture());
        assertThat(cap.getValue().getToken()).isEqualTo("valid.jwt.token");
        assertThat(cap.getValue().getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void logout_tokenExpiryStoredCorrectly() {
        LocalDateTime specificExpiry = LocalDateTime.of(2027, 1, 1, 12, 0, 0);
        when(jwtUtils.getExpiryFromToken(anyString())).thenReturn(specificExpiry);

        authService.logout("some.token");

        ArgumentCaptor<TokenBlacklist> cap = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(tokenBlacklistRepository).save(cap.capture());
        assertThat(cap.getValue().getExpiresAt()).isEqualTo(specificExpiry);
    }

    @Test
    void logout_alwaysCallsBlacklistSave() {
        when(jwtUtils.getExpiryFromToken(any())).thenReturn(LocalDateTime.now().plusHours(1));

        authService.logout("any.token");

        verify(tokenBlacklistRepository, times(1)).save(any(TokenBlacklist.class));
    }

    // ── helpers ───────────────────────────────────────────────

    private RegisterRequest makeRegisterRequest(String name, String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setName(name);
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    private LoginRequest makeLoginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }
}