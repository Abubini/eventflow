// src/test/java/com/ctbe/eventflow/controller/AuthControllerTest.java
package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.dto.request.LoginRequest;
import com.ctbe.eventflow.dto.request.RegisterRequest;
import com.ctbe.eventflow.dto.response.TokenResponse;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.ConflictException;
import com.ctbe.eventflow.model.UserRole;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.*;
import com.ctbe.eventflow.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    // ── POST /api/auth/register ───────────────────────────────

    @Test
    void register_validRequest_returns201WithUser() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice"); req.setEmail("alice@example.com"); req.setPassword("password123");
        UserDTO dto = UserDTO.builder().id(1L).name("Alice").email("alice@example.com")
                .role(UserRole.ATTENDEE).active(true).build();
        when(authService.register(any())).thenReturn(dto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("ATTENDEE"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice"); req.setEmail("alice@example.com"); req.setPassword("password123");
        when(authService.register(any())).thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice"); req.setEmail("not-an-email"); req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Alice"); req.setEmail("alice@example.com"); req.setPassword("short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingName_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("alice@example.com"); req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/login ──────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com"); req.setPassword("password123");
        TokenResponse response = TokenResponse.builder()
                .token("jwt.token.here").type("Bearer").expiresIn(86400000L)
                .user(UserDTO.builder().email("alice@example.com").build()).build();
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com"); req.setPassword("wrongpass");
        when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /api/auth/logout ─────────────────────────────────

    @Test
    @WithMockUser
    void logout_withBearerToken_returns200() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer some.jwt.token"))
                .andExpect(status().isOk());

        verify(authService).logout("some.jwt.token");
    }

    @Test
    @WithMockUser
    void logout_withoutToken_returns200AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        verify(authService, never()).logout(any());
    }
}