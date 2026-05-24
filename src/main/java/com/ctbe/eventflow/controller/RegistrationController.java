package com.ctbe.eventflow.controller;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/events/{eventId}") @RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    @PostMapping("/register")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<RegistrationDTO> register(@PathVariable Long eventId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(eventId));
    }
    @DeleteMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancel(@PathVariable Long eventId) {
        registrationService.cancel(eventId); return ResponseEntity.noContent().build();
    }
    @GetMapping("/attendees")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<Page<UserDTO>> getAttendees(
        @PathVariable Long eventId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(registrationService.getAttendees(eventId,PageRequest.of(page,Math.min(size,100))));
    }
}
