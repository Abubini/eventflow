package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.dto.request.CreateEventForOrganizerRequest;
import com.ctbe.eventflow.dto.request.CreateEventRequest;
import com.ctbe.eventflow.dto.request.UpdateEventRequest;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.model.EventStatus;
import com.ctbe.eventflow.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // ── Public: list published events ─────────────────────────

    @GetMapping("/api/events")
    public ResponseEntity<Page<EventDTO>> list(
            @RequestParam(defaultValue = "0")        int page,
            @RequestParam(defaultValue = "10")       int size,
            @RequestParam(defaultValue = "dateTime") String sort) {
        return ResponseEntity.ok(
                eventService.listPublished(
                        PageRequest.of(page, Math.min(size, 100), Sort.by(sort))));
    }

    // ── Public: get single event ──────────────────────────────

    @GetMapping("/api/events/{id}")
    public ResponseEntity<EventDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getById(id));
    }

    // ── Public: search ────────────────────────────────────────

    @GetMapping("/api/events/search")
    public ResponseEntity<List<EventDTO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                eventService.search(keyword, location, status, from, to));
    }

    // ── Organizer / Staff: create own event ───────────────────

    @PostMapping("/api/events")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<EventDTO> create(
            @Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.create(req));
    }

    // ── Staff only: create an event for a specific organizer ──

    /**
     * POST /api/admin/events
     *
     * Staff creates an event and assigns it to an existing organizer.
     * The event will appear as if the organizer created it themselves —
     * the organizer can then manage it normally.
     *
     * Body: { "organizerId": 5, "title": "...", "location": "...", ... }
     */
    @PostMapping("/api/admin/events")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<EventDTO> createForOrganizer(
            @Valid @RequestBody CreateEventForOrganizerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createForOrganizer(req));
    }

    // ── Organizer / Staff: update event ──────────────────────

    @PutMapping("/api/events/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<EventDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest req) {
        return ResponseEntity.ok(eventService.update(id, req));
    }

    // ── Organizer / Staff: delete event ──────────────────────

    @DeleteMapping("/api/events/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}