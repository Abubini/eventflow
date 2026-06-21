package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.OrganizerRequestForm;
import com.ctbe.eventflow.dto.request.ReviewRequest;
import com.ctbe.eventflow.dto.response.OrganizerRequestDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.OrganizerRequestMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerRequestService {

    private final OrganizerRequestRepository requestRepository;
    private final UserRepository             userRepository;
    private final OrganizerRequestMapper     requestMapper;
    private final EmailService               emailService;

    // ── Submit a request (attendee only) ──────────────────────

    @Transactional
    public OrganizerRequestDTO submit(OrganizerRequestForm form) {
        User user = currentUser();

        if (user.getRole() != UserRole.ATTENDEE)
            throw new BadRequestException(
                    "Only attendees can request to become an organizer");

        if (requestRepository.existsByUserAndStatus(user, RequestStatus.PENDING))
            throw new ConflictException(
                    "You already have a pending organizer request. " +
                            "Please wait for it to be reviewed before submitting another.");

        OrganizerRequest req = OrganizerRequest.builder()
                .user(user)
                .name(form.getName())
                .email(form.getEmail())
                .phone(form.getPhone())
                .message(form.getMessage())
                .status(RequestStatus.PENDING)
                .build();

        OrganizerRequest saved = requestRepository.save(req);

        // Email every staff member
        List<User> staffMembers = userRepository.findByRole(UserRole.STAFF);
        for (User staff : staffMembers) {
            emailService.sendOrganizerRequestNotificationToStaff(
                    staff.getEmail(), staff.getName(), saved);
        }

        return requestMapper.toDTO(saved);
    }

    // ── My own requests (attendee) ────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrganizerRequestDTO> getMyRequests(Pageable pageable) {
        User user = currentUser();
        return requestRepository.findByUser(user, pageable)
                .map(requestMapper::toDTO);
    }

    // ── List all requests (staff) ─────────────────────────────

    @Transactional(readOnly = true)
    public Page<OrganizerRequestDTO> listAll(RequestStatus status, Pageable pageable) {
        if (status != null) {
            return requestRepository.findByStatus(status, pageable)
                    .map(requestMapper::toDTO);
        }
        return requestRepository.findAll(pageable)
                .map(requestMapper::toDTO);
    }

    // ── Get single request (staff) ────────────────────────────

    @Transactional(readOnly = true)
    public OrganizerRequestDTO getById(Long id) {
        return requestMapper.toDTO(findOrThrow(id));
    }

    // ── Review a request (staff: approve or decline) ──────────

    @Transactional
    public OrganizerRequestDTO review(Long requestId, ReviewRequest decision) {
        User reviewer = currentUser();
        OrganizerRequest req = findOrThrow(requestId);

        if (req.getStatus() != RequestStatus.PENDING)
            throw new BadRequestException(
                    "This request has already been " + req.getStatus().name().toLowerCase());

        if (decision.getDecision() == RequestStatus.PENDING)
            throw new BadRequestException("Decision must be APPROVED or DECLINED");

        req.setStatus(decision.getDecision());
        req.setReviewedBy(reviewer);
        req.setReviewNote(decision.getNote());
        req.setReviewedAt(LocalDateTime.now());

        if (decision.getDecision() == RequestStatus.APPROVED) {
            // Promote the user to ORGANIZER
            User applicant = req.getUser();
            applicant.setRole(UserRole.ORGANIZER);
            userRepository.save(applicant);
            emailService.sendRequestApproved(applicant, req);
        } else {
            emailService.sendRequestDeclined(req.getUser(), req);
        }

        return requestMapper.toDTO(requestRepository.save(req));
    }

    // ── Helpers ───────────────────────────────────────────────

    private OrganizerRequest findOrThrow(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organizer request not found: " + id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}