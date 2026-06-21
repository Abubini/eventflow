package com.ctbe.eventflow.controller;

import com.ctbe.eventflow.dto.request.OrganizerRequestForm;
import com.ctbe.eventflow.dto.request.ReviewRequest;
import com.ctbe.eventflow.dto.response.OrganizerRequestDTO;
import com.ctbe.eventflow.model.RequestStatus;
import com.ctbe.eventflow.service.OrganizerRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrganizerRequestController {

    private final OrganizerRequestService orgRequestService;

    // ── Attendee: submit a request ────────────────────────────

    /**
     * POST /api/organizer-requests
     *
     * Any authenticated attendee can submit a form to request
     * organizer privileges. A duplicate pending request is rejected.
     */
    @PostMapping("/api/organizer-requests")
    @PreAuthorize("hasRole('ATTENDEE')")
    public ResponseEntity<OrganizerRequestDTO> submit(
            @Valid @RequestBody OrganizerRequestForm form) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orgRequestService.submit(form));
    }

    // ── Attendee: view own requests ───────────────────────────

    @GetMapping("/api/organizer-requests/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrganizerRequestDTO>> getMyRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                orgRequestService.getMyRequests(PageRequest.of(page, Math.min(size, 50))));
    }

    // ── Staff: list all requests ──────────────────────────────

    /**
     * GET /api/admin/organizer-requests?status=PENDING
     *
     * Optional ?status= filter: PENDING | APPROVED | DECLINED
     */
    @GetMapping("/api/admin/organizer-requests")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Page<OrganizerRequestDTO>> listAll(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                orgRequestService.listAll(status, PageRequest.of(page, Math.min(size, 100))));
    }

    // ── Staff: get single request ─────────────────────────────

    @GetMapping("/api/admin/organizer-requests/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<OrganizerRequestDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orgRequestService.getById(id));
    }

    // ── Staff: approve or decline ─────────────────────────────

    /**
     * POST /api/admin/organizer-requests/{id}/review
     * Body: { "decision": "APPROVED", "note": "Looks good!" }
     *    or { "decision": "DECLINED", "note": "Insufficient details." }
     *
     * When APPROVED: the user's role is changed to ORGANIZER automatically.
     * Both decisions trigger an email to the applicant.
     */
    @PostMapping("/api/admin/organizer-requests/{id}/review")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<OrganizerRequestDTO> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest decision) {
        return ResponseEntity.ok(orgRequestService.review(id, decision));
    }
}