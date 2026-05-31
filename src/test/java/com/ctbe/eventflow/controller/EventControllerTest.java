// src/test/java/com/ctbe/eventflow/controller/EventControllerTest.java
package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.dto.request.CreateEventRequest;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.exception.ForbiddenException;
import com.ctbe.eventflow.exception.ResourceNotFoundException;
import com.ctbe.eventflow.model.EventStatus;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.*;
import com.ctbe.eventflow.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class EventControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean EventService eventService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    private ObjectMapper objectMapper;
    private EventDTO sampleEvent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sampleEvent = EventDTO.builder()
                .id(1L).title("Test Event").location("Addis Ababa")
                .status(EventStatus.PUBLISHED)
                .dateTime(LocalDateTime.now().plusDays(5))
                .build();
    }

    // ── GET /api/events ───────────────────────────────────────

    @Test
    void listEvents_noAuth_returns200() throws Exception {
        when(eventService.listPublished(any())).thenReturn(new PageImpl<>(List.of(sampleEvent)));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Test Event"));
    }

    @Test
    void listEvents_customPagination_passedToService() throws Exception {
        when(eventService.listPublished(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/events").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());
    }

    // ── GET /api/events/{id} ──────────────────────────────────

    @Test
    void getById_existingEvent_returns200() throws Exception {
        when(eventService.getById(1L)).thenReturn(sampleEvent);

        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_nonExistent_returns404() throws Exception {
        when(eventService.getById(99L)).thenThrow(new ResourceNotFoundException("Event not found: 99"));

        mockMvc.perform(get("/api/events/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/events/search ────────────────────────────────

    @Test
    void search_withKeyword_returns200() throws Exception {
        when(eventService.search(eq("Tech"), any(), any(), any(), any()))
                .thenReturn(List.of(sampleEvent));

        mockMvc.perform(get("/api/events/search").param("keyword", "Tech"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void search_noParams_returns200() throws Exception {
        when(eventService.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/events/search"))
                .andExpect(status().isOk());
    }

    // ── POST /api/events ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void createEvent_asOrganizer_returns201() throws Exception {
        when(eventService.create(any())).thenReturn(sampleEvent);

        String body = "{\"title\":\"New Event\",\"location\":\"Addis\",\"dateTime\":\"2027-06-01T10:00:00\"}";
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void createEvent_asStaff_returns201() throws Exception {
        when(eventService.create(any())).thenReturn(sampleEvent);

        String body = "{\"title\":\"New Event\",\"location\":\"Addis\",\"dateTime\":\"2027-06-01T10:00:00\"}";
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void createEvent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"location\":\"L\",\"dateTime\":\"2027-01-01T10:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEvent_asAttendee_returns403() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"location\":\"L\",\"dateTime\":\"2027-01-01T10:00:00\"}")
                        .with(SecurityMockMvcRequestPostProcessors.user("att@test.com").roles("ATTENDEE")))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/events/{id} ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void updateEvent_asOrganizer_returns200() throws Exception {
        when(eventService.update(eq(1L), any())).thenReturn(sampleEvent);

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void updateEvent_forbidden_returns403() throws Exception {
        when(eventService.update(eq(1L), any())).thenThrow(new ForbiddenException("Not your event"));

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijack\"}"))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/events/{id} ───────────────────────────────

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void deleteEvent_asOrganizer_returns204() throws Exception {
        doNothing().when(eventService).delete(1L);

        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEvent_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isUnauthorized());
    }
}