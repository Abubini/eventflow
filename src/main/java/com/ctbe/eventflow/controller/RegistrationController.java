package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.dto.request.ScanRequest;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    // ── Register for an event ─────────────────────────────────

    @PostMapping("/api/events/{eventId}/register")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<RegistrationDTO> register(@PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(eventId));
    }

    // ── Cancel registration ───────────────────────────────────

    @DeleteMapping("/api/events/{eventId}/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(@PathVariable Long eventId) {
        registrationService.cancel(eventId);
        return ResponseEntity.noContent().build();
    }

    // ── My registered events (attendee) ──────────────────────

    /**
     * Returns a paginated list of the current user's registrations,
     * newest first. Each item includes the ticket code so the
     * attendee can navigate to the ticket endpoint for the QR code.
     */
    @GetMapping("/api/users/me/registrations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RegistrationDTO>> getMyRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                registrationService.getMyRegistrations(PageRequest.of(page, Math.min(size, 100))));
    }

    // ── Get ticket with QR code ───────────────────────────────

    /**
     * Returns the full ticket for the current user's registration to
     * the given event, including a base64-encoded QR code PNG.
     *
     * Frontend usage:
     *   <img src="data:image/png;base64,{ticket.qrCodeBase64}" />
     */
    @GetMapping("/api/events/{eventId}/ticket")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long eventId) {
        return ResponseEntity.ok(registrationService.getTicket(eventId));
    }

    // ── Scan a ticket (organizer / staff) ─────────────────────

    /**
     * Organizer or staff POSTs the UUID from a scanned QR code.
     * Returns a ScanResultDTO with valid=true/false and a human-readable message.
     *
     * Business rules enforced:
     *  - Ticket must exist
     *  - Ticket must belong to THIS event
     *  - Registration must not be cancelled
     *  - Ticket must not have been scanned before (one-time entry)
     */
    @PostMapping("/api/events/{eventId}/scan")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<ScanResultDTO> scanTicket(
            @PathVariable Long eventId,
            @Valid @RequestBody ScanRequest request) {
        return ResponseEntity.ok(registrationService.scanTicket(eventId, request));
    }

    // ── Attendees list ────────────────────────────────────────

    @GetMapping("/api/events/{eventId}/attendees")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<Page<UserDTO>> getAttendees(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                registrationService.getAttendees(eventId, PageRequest.of(page, Math.min(size, 100))));
    }
}