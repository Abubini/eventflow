// src/test/java/com/ctbe/eventflow/controller/RegistrationControllerTest.java
package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.dto.response.RegistrationDTO;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.BadRequestException;
import com.ctbe.eventflow.exception.ConflictException;
import com.ctbe.eventflow.model.RegStatus;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.*;
import com.ctbe.eventflow.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistrationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class RegistrationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean RegistrationService registrationService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    // ── POST /api/events/{id}/register ────────────────────────

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_asAttendee_returns201() throws Exception {
        RegistrationDTO dto = RegistrationDTO.builder().id(1L).eventId(1L).status(RegStatus.CONFIRMED).build();
        when(registrationService.register(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_alreadyRegistered_returns409() throws Exception {
        when(registrationService.register(1L)).thenThrow(new ConflictException("Already registered"));

        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_fullCapacity_returns400() throws Exception {
        when(registrationService.register(1L)).thenThrow(new BadRequestException("Event is at full capacity"));

        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void register_asOrganizer_returns403() throws Exception {
        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/events/{id}/register ─────────────────────

    @Test
    @WithMockUser
    void cancel_authenticated_returns204() throws Exception {
        doNothing().when(registrationService).cancel(1L);

        mockMvc.perform(delete("/api/events/1/register"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancel_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/events/1/register"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/events/{id}/attendees ────────────────────────

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void getAttendees_asOrganizer_returns200() throws Exception {
        Page<UserDTO> page = new PageImpl<>(List.of(UserDTO.builder().id(2L).name("Bob").build()));
        when(registrationService.getAttendees(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/events/1/attendees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bob"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void getAttendees_asStaff_returns200() throws Exception {
        when(registrationService.getAttendees(eq(1L), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/events/1/attendees"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void getAttendees_asAttendee_returns403() throws Exception {
        mockMvc.perform(get("/api/events/1/attendees"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAttendees_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/events/1/attendees"))
                .andExpect(status().isForbidden()); // ← Spring returns 403 for @PreAuthorize with no auth
    }
}