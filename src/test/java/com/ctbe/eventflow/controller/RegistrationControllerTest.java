// src/test/java/com/ctbe/eventflow/controller/RegistrationControllerTest.java
package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.config.SecurityConfig;
import com.ctbe.eventflow.dto.request.BookingRequest;
import com.ctbe.eventflow.dto.request.ScanRequest;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.exception.BadRequestException;
import com.ctbe.eventflow.exception.ConflictException;
import com.ctbe.eventflow.model.RegStatus;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import com.ctbe.eventflow.security.*;
import com.ctbe.eventflow.service.RegistrationService;
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

@WebMvcTest(RegistrationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class RegistrationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RegistrationService registrationService;
    @MockBean JwtUtils jwtUtils;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean TokenBlacklistRepository tokenBlacklistRepository;

    // ── POST /api/events/{id}/register ────────────────────────

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_asAttendee_returns201() throws Exception {
        RegistrationDTO dto = RegistrationDTO.builder().id(1L).eventId(1L).status(RegStatus.CONFIRMED).build();
        when(registrationService.register(eq(1L), any(BookingRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/events/1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_noBody_returns201() throws Exception {
        // Controller accepts a null/missing body and defaults to new BookingRequest()
        RegistrationDTO dto = RegistrationDTO.builder().id(1L).eventId(1L).status(RegStatus.CONFIRMED).build();
        when(registrationService.register(eq(1L), any(BookingRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_alreadyRegistered_returns409() throws Exception {
        when(registrationService.register(eq(1L), any(BookingRequest.class)))
                .thenThrow(new ConflictException("Already registered"));

        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void register_fullCapacity_returns400() throws Exception {
        when(registrationService.register(eq(1L), any(BookingRequest.class)))
                .thenThrow(new BadRequestException("Event is at full capacity"));

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
    void register_asOrganizer_returns201() throws Exception {
        // Controller allows ATTENDEE or ORGANIZER
        RegistrationDTO dto = RegistrationDTO.builder().id(1L).eventId(1L).status(RegStatus.CONFIRMED).build();
        when(registrationService.register(eq(1L), any(BookingRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/events/1/register"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void register_asStaffOnly_returns403() throws Exception {
        // STAFF is not permitted on the self-service register endpoint
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

    // ── GET /api/users/me/registrations ───────────────────────

    @Test
    @WithMockUser
    void getMyRegistrations_authenticated_returns200() throws Exception {
        Page<RegistrationDTO> page = new PageImpl<>(
                List.of(RegistrationDTO.builder().id(1L).eventId(1L).status(RegStatus.CONFIRMED).build()));
        when(registrationService.getMyRegistrations(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/users/me/registrations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser
    void getMyRegistrations_sizeAboveCap_isClampedTo100() throws Exception {
        when(registrationService.getMyRegistrations(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/users/me/registrations").param("size", "500"))
                .andExpect(status().isOk());

        verify(registrationService).getMyRegistrations(argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void getMyRegistrations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/registrations"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/events/{id}/ticket ───────────────────────────

    @Test
    @WithMockUser
    void getTicket_authenticated_returns200() throws Exception {
        TicketDTO ticket = TicketDTO.builder().eventId(1L).qrCodeBase64("base64png").build();
        when(registrationService.getTicket(1L)).thenReturn(ticket);

        mockMvc.perform(get("/api/events/1/ticket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrCodeBase64").value("base64png"));
    }



    // ── POST /api/events/{id}/scan ────────────────────────────







    @Test
    void scanTicket_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/events/1/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketUuid\":\"some-uuid\"}"))
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

    // ── POST /api/events/{id}/waitlist ────────────────────────

    @Test
    @WithMockUser
    void joinWaitlist_authenticated_returns201() throws Exception {
        WaitlistDTO dto = WaitlistDTO.builder().id(1L).eventId(1L).build();
        when(registrationService.joinWaitlist(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/events/1/waitlist"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(1));
    }

    @Test
    void joinWaitlist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/events/1/waitlist"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/events/{id}/waitlist ──────────────────────

    @Test
    @WithMockUser
    void leaveWaitlist_authenticated_returns204() throws Exception {
        doNothing().when(registrationService).leaveWaitlist(1L);

        mockMvc.perform(delete("/api/events/1/waitlist"))
                .andExpect(status().isNoContent());
    }

    @Test
    void leaveWaitlist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/events/1/waitlist"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/users/me/waitlist ────────────────────────────

    @Test
    @WithMockUser
    void getMyWaitlistEntries_authenticated_returns200() throws Exception {
        when(registrationService.getMyWaitlistEntries())
                .thenReturn(List.of(WaitlistDTO.builder().id(1L).eventId(1L).build()));

        mockMvc.perform(get("/api/users/me/waitlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(1));
    }

    @Test
    void getMyWaitlistEntries_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/waitlist"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/admin/events/{id}/register/{userId} ─────────

    @Test
    @WithMockUser(roles = "STAFF")
    void adminRegister_asStaff_returns201() throws Exception {
        RegistrationDTO dto = RegistrationDTO.builder().id(1L).eventId(1L).status(RegStatus.CONFIRMED).build();
        when(registrationService.adminRegister(eq(1L), eq(2L), any(BookingRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/events/1/register/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void adminRegister_asOrganizer_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/events/1/register/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void adminRegister_asAttendee_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/events/1/register/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRegister_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/events/1/register/2"))
                .andExpect(status().isUnauthorized());
    }


    // ── GET /api/events/{id}/ticket ───────────────────────────

    @Test
    void getTicket_unauthenticated_returns403() throws Exception {
        // GET /api/events/** is permitAll() at the security-filter level,
        // so this is denied by @PreAuthorize("isAuthenticated()") → 403, not 401
        mockMvc.perform(get("/api/events/1/ticket"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/events/{id}/scan ────────────────────────────

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void scanTicket_asOrganizer_returnsValidResult() throws Exception {
        ScanResultDTO result = ScanResultDTO.builder().valid(true).message("Entry granted").build();
        when(registrationService.scanTicket(eq(1L), any(ScanRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/events/1/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\":\"" + java.util.UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void scanTicket_asStaff_returns200() throws Exception {
        ScanResultDTO result = ScanResultDTO.builder().valid(false).message("Already scanned").build();
        when(registrationService.scanTicket(eq(1L), any(ScanRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/events/1/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\":\"" + java.util.UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @WithMockUser(roles = "ATTENDEE")
    void scanTicket_asAttendee_returns403() throws Exception {
        mockMvc.perform(post("/api/events/1/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticketCode\":\"" + java.util.UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void scanTicket_missingTicketCode_returns400() throws Exception {
        mockMvc.perform(post("/api/events/1/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}