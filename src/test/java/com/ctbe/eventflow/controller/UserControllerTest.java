// src/test/java/com/ctbe/eventflow/controller/UserControllerTest.java
package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.ResourceNotFoundException;
import com.ctbe.eventflow.model.UserRole;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.*;
import com.ctbe.eventflow.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    private UserDTO sampleUser() {
        return UserDTO.builder().id(1L).name("Alice").email("alice@example.com")
                .role(UserRole.ATTENDEE).active(true).build();
    }

    // ── GET /api/users/me ─────────────────────────────────────

    @Test
    @WithMockUser
    void getProfile_authenticated_returns200() throws Exception {
        when(userService.getProfile()).thenReturn(sampleUser());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/users/me ─────────────────────────────────────

    @Test
    @WithMockUser
    void updateProfile_validRequest_returns200() throws Exception {
        when(userService.updateProfile(any())).thenReturn(sampleUser());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateProfile_missingName_returns400() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/admin/users ──────────────────────────────────

    @Test
    @WithMockUser(roles = "STAFF")
    void listUsers_asStaff_returns200() throws Exception {
        Page<UserDTO> page = new PageImpl<>(List.of(sampleUser()));
        when(userService.listAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"));
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void listUsers_asAttendee_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── PATCH /api/admin/users/{id}/role ──────────────────────

    @Test
    @WithMockUser(roles = "STAFF")
    void updateRole_asStaff_returns200() throws Exception {
        when(userService.updateRole(eq(1L), any())).thenReturn(
                UserDTO.builder().id(1L).role(UserRole.ORGANIZER).build());

        mockMvc.perform(patch("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void updateRole_userNotFound_returns404() throws Exception {
        when(userService.updateRole(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("User not found: 99"));

        mockMvc.perform(patch("/api/admin/users/99/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void updateRole_asOrganizer_returns403() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ATTENDEE\"}"))
                .andExpect(status().isForbidden());
    }
}