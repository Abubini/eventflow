package com.ctbe.eventflow.controller;
import com.ctbe.eventflow.dto.request.CreateScheduleRequest;
import com.ctbe.eventflow.dto.response.ScheduleDTO;
import com.ctbe.eventflow.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/events/{eventId}/schedules") @RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;
    @GetMapping
    public ResponseEntity<List<ScheduleDTO>> getSchedules(@PathVariable Long eventId) {
        return ResponseEntity.ok(scheduleService.getSchedules(eventId));
    }
    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('STAFF')")
    public ResponseEntity<ScheduleDTO> addSession(@PathVariable Long eventId, @Valid @RequestBody CreateScheduleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.addSession(eventId,req));
    }
}
