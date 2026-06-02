// src/test/java/com/ctbe/eventflow/security/SecurityIntegrationTest.java
package com.ctbe.eventflow.security;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.service.EventService;
import com.ctbe.eventflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        com.ctbe.eventflow.controller.EventController.class,
        com.ctbe.eventflow.controller.UserController.class
})
@Import({SecurityConfig.class, JwtAuthFilter.class})
class SecurityIntegrationTest {

    @Autowired MockMvc mockMvc;

    @MockBean EventService eventService;
    @MockBean UserService userService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    // ── Public endpoints ──────────────────────────────────────

    @Test
    void publicGetEvents_noAuth_returns200() throws Exception {
        when(eventService.listPublished(any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/events")).andExpect(status().isOk());
    }

    @Test
    void publicGetEventById_noAuth_returns200() throws Exception {
        when(eventService.getById(1L)).thenReturn(
                com.ctbe.eventflow.dto.response.EventDTO.builder().id(1L).title("T").build());
        mockMvc.perform(get("/api/events/1")).andExpect(status().isOk());
    }

    // ── Protected endpoints - unauthenticated ─────────────────

    @Test
    void protectedPost_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType("application/json")
                        .content("{\"title\":\"T\",\"location\":\"L\",\"dateTime\":\"2027-01-01T10:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    // ── Role enforcement ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void attendeeAccessesOrganizerEndpoint_returns403() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType("application/json")
                        .content("{\"title\":\"T\",\"location\":\"L\",\"dateTime\":\"2027-06-01T10:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void attendeeAccessesAdminEndpoint_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffAccessesAdminEndpoint_returns200() throws Exception {
        when(userService.listAll(any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizerAccessesProtectedEventGet_returns200() throws Exception {
        when(eventService.listPublished(any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/events")).andExpect(status().isOk());
    }
}