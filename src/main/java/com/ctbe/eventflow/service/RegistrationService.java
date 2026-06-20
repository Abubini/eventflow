package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.ScanRequest;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.*;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final TicketQrService ticketQrService;

    // ── Register ──────────────────────────────────────────────

    @Transactional
    public RegistrationDTO register(Long eventId) {
        User user = currentUser();
        Event event = findEventOrThrow(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new BadRequestException("Event is not open for registration");

        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId))
            throw new ConflictException("Already registered for this event");

        if (event.getCapacity() != null) {
            long confirmed = registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED);
            if (confirmed >= event.getCapacity())
                throw new BadRequestException("Event is at full capacity");
        }

        Registration reg = Registration.builder()
                .user(user)
                .event(event)
                .status(RegStatus.CONFIRMED)
                .build();

        return registrationMapper.toDTO(registrationRepository.save(reg));
    }

    // ── Cancel ────────────────────────────────────────────────

    @Transactional
    public void cancel(Long eventId) {
        User user = currentUser();
        Event event = findEventOrThrow(eventId);
        Registration reg = registrationRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));
        registrationRepository.delete(reg);
    }

    // ── My registrations (attendee) ───────────────────────────

    @Transactional(readOnly = true)
    public Page<RegistrationDTO> getMyRegistrations(Pageable pageable) {
        User user = currentUser();
        return registrationRepository
                .findByUserOrderByRegisteredAtDesc(user, pageable)
                .map(registrationMapper::toDTO);
    }

    // ── Get ticket with QR code ───────────────────────────────

    @Transactional(readOnly = true)
    public TicketDTO getTicket(Long eventId) {
        User user = currentUser();
        Event event = findEventOrThrow(eventId);

        Registration reg = registrationRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not registered for this event"));

        String qr = ticketQrService.generateQrCodeBase64(reg.getTicketCode());
        return registrationMapper.toTicketDTO(reg, qr);
    }

    // ── Scan ticket (organizer / staff) ──────────────────────

    @Transactional
    public ScanResultDTO scanTicket(Long eventId, ScanRequest request) {
        User scanner = currentUser();
        Event event = findEventOrThrow(eventId);

        // Only the event organizer or STAFF may scan
        if (!event.getCreatedBy().getId().equals(scanner.getId())
                && scanner.getRole() != UserRole.STAFF) {
            throw new ForbiddenException("Only the event organizer or staff can scan tickets");
        }

        UUID ticketCode = request.getTicketCode();
        Registration reg = registrationRepository.findByTicketCode(ticketCode)
                .orElse(null);

        // Ticket not found at all
        if (reg == null) {
            return ScanResultDTO.builder()
                    .valid(false)
                    .message("Invalid ticket: QR code not recognised")
                    .ticketCode(ticketCode)
                    .build();
        }

        // Ticket belongs to a different event
        if (!reg.getEvent().getId().equals(eventId)) {
            return ScanResultDTO.builder()
                    .valid(false)
                    .message("Invalid ticket: this ticket is for a different event")
                    .ticketCode(ticketCode)
                    .build();
        }

        // Registration was cancelled
        if (reg.getStatus() == RegStatus.CANCELLED) {
            return ScanResultDTO.builder()
                    .valid(false)
                    .message("Invalid ticket: registration has been cancelled")
                    .ticketCode(ticketCode)
                    .attendeeName(reg.getUser().getName())
                    .attendeeEmail(reg.getUser().getEmail())
                    .eventTitle(event.getTitle())
                    .eventDateTime(event.getDateTime())
                    .build();
        }

        // Already scanned
        if (reg.isScanned()) {
            return ScanResultDTO.builder()
                    .valid(false)
                    .message("Ticket already scanned at " + reg.getScannedAt())
                    .ticketCode(ticketCode)
                    .attendeeName(reg.getUser().getName())
                    .attendeeEmail(reg.getUser().getEmail())
                    .eventTitle(event.getTitle())
                    .eventDateTime(event.getDateTime())
                    .scannedAt(reg.getScannedAt())
                    .build();
        }

        // ✅ Valid — mark as scanned
        reg.setScanned(true);
        reg.setScannedAt(LocalDateTime.now());
        registrationRepository.save(reg);

        return ScanResultDTO.builder()
                .valid(true)
                .message("✅ Valid ticket — entry approved")
                .ticketCode(ticketCode)
                .attendeeName(reg.getUser().getName())
                .attendeeEmail(reg.getUser().getEmail())
                .eventTitle(event.getTitle())
                .eventDateTime(event.getDateTime())
                .scannedAt(reg.getScannedAt())
                .build();
    }

    // ── Attendees list (organizer / staff) ────────────────────

    @Transactional(readOnly = true)
    public Page<UserDTO> getAttendees(Long eventId, Pageable pageable) {
        Event event = findEventOrThrow(eventId);
        User current = currentUser();

        if (!event.getCreatedBy().getId().equals(current.getId())
                && current.getRole() != UserRole.STAFF) {
            throw new ForbiddenException("Access denied");
        }

        return registrationRepository.findByEvent(event, pageable)
                .map(r -> userMapper.toDTO(r.getUser()));
    }

    // ── Helpers ───────────────────────────────────────────────

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}