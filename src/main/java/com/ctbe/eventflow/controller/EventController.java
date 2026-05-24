package com.ctbe.eventflow.controller;
import com.ctbe.eventflow.dto.request.*;
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
@RestController @RequestMapping("/api/events") @RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    @GetMapping
    public ResponseEntity<Page<EventDTO>> list(
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size,
        @RequestParam(defaultValue="dateTime") String sort) {
        return ResponseEntity.ok(eventService.listPublished(PageRequest.of(page,Math.min(size,100),Sort.by(sort))));
    }
    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getById(@PathVariable Long id) { return ResponseEntity.ok(eventService.getById(id)); }
    @GetMapping("/search")
    public ResponseEntity<List<EventDTO>> search(
        @RequestParam(required=false) String keyword, @RequestParam(required=false) String location,
        @RequestParam(required=false) EventStatus status,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(eventService.search(keyword,location,status,from,to));
    }
    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<EventDTO> create(@Valid @RequestBody CreateEventRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(req));
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<EventDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateEventRequest req) {
        return ResponseEntity.ok(eventService.update(id,req));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<Void> delete(@PathVariable Long id) { eventService.delete(id); return ResponseEntity.noContent().build(); }
}
