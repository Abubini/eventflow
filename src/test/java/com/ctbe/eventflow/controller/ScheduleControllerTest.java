// src/test/java/com/ctbe/eventflow/controller/ScheduleControllerTest.java
package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.dto.response.ScheduleDTO;
import com.ctbe.eventflow.exception.BadRequestException;
import com.ctbe.eventflow.exception.ForbiddenException;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.*;
import com.ctbe.eventflow.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScheduleController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ScheduleControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ScheduleService scheduleService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    // ── GET /api/events/{id}/schedules ────────────────────────

    @Test
    void getSchedules_noAuth_returns200() throws Exception {
        ScheduleDTO dto = ScheduleDTO.builder().id(1L).sessionTitle("Keynote")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2)).build();
        when(scheduleService.getSchedules(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/events/1/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionTitle").value("Keynote"));
    }

    @Test
    void getSchedules_emptyResult_returns200EmptyArray() throws Exception {
        when(scheduleService.getSchedules(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/events/1/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── POST /api/events/{id}/schedules ───────────────────────

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void addSession_asOrganizer_returns201() throws Exception {
        ScheduleDTO dto = ScheduleDTO.builder().id(5L).sessionTitle("Keynote").build();
        when(scheduleService.addSession(eq(1L), any())).thenReturn(dto);

        String body = "{\"sessionTitle\":\"Keynote\",\"startTime\":\"2027-06-01T09:00:00\",\"endTime\":\"2027-06-01T11:00:00\"}";
        mockMvc.perform(post("/api/events/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void addSession_endBeforeStart_returns400() throws Exception {
        when(scheduleService.addSession(eq(1L), any()))
                .thenThrow(new BadRequestException("End time must be after start time"));

        String body = "{\"sessionTitle\":\"Bad\",\"startTime\":\"2027-06-01T11:00:00\",\"endTime\":\"2027-06-01T09:00:00\"}";
        mockMvc.perform(post("/api/events/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void addSession_notOrganizer_returns403() throws Exception {
        when(scheduleService.addSession(eq(1L), any()))
                .thenThrow(new ForbiddenException("Only the organizer can add sessions"));

        String body = "{\"sessionTitle\":\"X\",\"startTime\":\"2027-06-01T09:00:00\",\"endTime\":\"2027-06-01T11:00:00\"}";
        mockMvc.perform(post("/api/events/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void addSession_unauthenticated_returns401() throws Exception {
        String body = "{\"sessionTitle\":\"X\",\"startTime\":\"2027-06-01T09:00:00\",\"endTime\":\"2027-06-01T11:00:00\"}";
        mockMvc.perform(post("/api/events/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void addSession_asAttendee_returns403() throws Exception {
        String body = "{\"sessionTitle\":\"X\",\"startTime\":\"2027-06-01T09:00:00\",\"endTime\":\"2027-06-01T11:00:00\"}";
        mockMvc.perform(post("/api/events/1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}