package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.BookingRequest;
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
import java.util.List;
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
    private final EmailService emailService;
    private final WaitlistRepository waitlistRepository;


    // ── Register ──────────────────────────────────────────────

    @Transactional
    public RegistrationDTO register(Long eventId, BookingRequest req) {
        User  user  = currentUser();
        Event event = findEventOrThrow(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new BadRequestException("Event is not open for registration");

        // Organizers may register for other organizers' events, but NOT their own
        if (user.getRole() == UserRole.ORGANIZER
                && event.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException(
                    "You cannot register for your own event");
        }

        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId))
            throw new ConflictException("Already registered for this event");

        int seats = req.getAttendeeCount();

        if (event.getCapacity() != null) {
            long used = registrationRepository
                    .sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED);
            long available = event.getCapacity() - used;
            if (available <= 0)
                throw new BadRequestException("Event is at full capacity");
            if (seats > available)
                throw new BadRequestException(
                        "Only " + available + " seat(s) left. " +
                                "Please reduce your attendee count.");
        }

        Registration reg = Registration.builder()
                .user(user)
                .event(event)
                .status(RegStatus.CONFIRMED)
                .attendeeCount(seats)
                .build();

        Registration saved = registrationRepository.save(reg);
        emailService.sendBookingConfirmation(saved);
        return registrationMapper.toDTO(saved);
    }

    // ── Cancel / Release booking ──────────────────────────────

    @Transactional
    public void cancel(Long eventId) {
        User  user  = currentUser();
        Event event = findEventOrThrow(eventId);

        if (!LocalDateTime.now().isBefore(event.getDateTime()))
            throw new BadRequestException(
                    "Cannot cancel a booking after the event has started");

        Registration reg = registrationRepository
                .findByUserAndEvent(user, event)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        int releasedSeats = reg.getAttendeeCount();
        registrationRepository.delete(reg);

        emailService.sendCancellationConfirmation(user, event, releasedSeats);
        notifyWaitlist(event, releasedSeats);
    }

    // ── My registrations ──────────────────────────────────────

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
        Event event   = findEventOrThrow(eventId);
        User  current = currentUser();
        if (!event.getCreatedBy().getId().equals(current.getId())
                && current.getRole() != UserRole.STAFF)
            throw new ForbiddenException("Access denied");
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


    // ── Waitlist: join ────────────────────────────────────────

    @Transactional
    public WaitlistDTO joinWaitlist(Long eventId) {
        User  user  = currentUser();
        Event event = findEventOrThrow(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new BadRequestException(
                    "Cannot join waitlist for an event that is not published");

        if (registrationRepository.existsByUserIdAndEventId(user.getId(), eventId))
            throw new ConflictException("You are already registered for this event");

        if (waitlistRepository.existsByUserIdAndEventId(user.getId(), eventId))
            throw new ConflictException("You are already on the waitlist for this event");

        if (event.getCapacity() != null) {
            long used = registrationRepository
                    .sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED);
            if (used < event.getCapacity())
                throw new BadRequestException(
                        "There are still seats available — you can book directly");
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .user(user)
                .event(event)
                .build();

        WaitlistEntry saved = waitlistRepository.save(entry);
        emailService.sendWaitlistConfirmation(user, event);
        return registrationMapper.toWaitlistDTO(saved);
    }

    // ── Waitlist: leave ───────────────────────────────────────

    @Transactional
    public void leaveWaitlist(Long eventId) {
        User  user  = currentUser();
        Event event = findEventOrThrow(eventId);
        WaitlistEntry entry = waitlistRepository.findByUserAndEvent(user, event)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You are not on the waitlist for this event"));
        waitlistRepository.delete(entry);
    }

    // ── My waitlist entries ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<WaitlistDTO> getMyWaitlistEntries() {
        User user = currentUser();
        return waitlistRepository.findAll().stream()
                .filter(w -> w.getUser().getId().equals(user.getId()))
                .map(registrationMapper::toWaitlistDTO)
                .toList();
    }
    // ── Admin register (staff only) ───────────────────────────

    @Transactional
    public RegistrationDTO adminRegister(Long eventId, Long userId, BookingRequest req) {
        User  user  = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Event event = findEventOrThrow(eventId);

        if (registrationRepository.existsByUserIdAndEventId(userId, eventId))
            throw new ConflictException("User is already registered for this event");

        int seats = req.getAttendeeCount();
        if (event.getCapacity() != null) {
            long used = registrationRepository
                    .sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED);
            long available = event.getCapacity() - used;
            if (available <= 0)
                throw new BadRequestException("Event is at full capacity");
            if (seats > available)
                throw new BadRequestException("Only " + available + " seat(s) left.");
        }

        Registration reg = Registration.builder()
                .user(user)
                .event(event)
                .status(RegStatus.CONFIRMED)
                .attendeeCount(seats)
                .build();

        Registration saved = registrationRepository.save(reg);
        emailService.sendBookingConfirmation(saved);
        return registrationMapper.toDTO(saved);
    }

    // ── Internal: notify waitlist after slots open ────────────

    /**
     * Called whenever seats are freed (cancellation or capacity increase).
     * Notifies only as many waitlist users as there are available seats,
     * marks them as notified so they don't get duplicate emails.
     */
    public void notifyWaitlist(Event event, int releasedSeats) {
        if (event.getCapacity() == null) return; // unlimited — no waitlist needed

        List<WaitlistEntry> pending = waitlistRepository.findPendingByEvent(event);
        if (pending.isEmpty()) return;

        int toNotify = Math.min(releasedSeats, pending.size());
        for (int i = 0; i < toNotify; i++) {
            WaitlistEntry entry = pending.get(i);
            entry.setNotified(true);
            waitlistRepository.save(entry);
            emailService.sendSlotAvailableNotification(entry.getUser(), event);
        }
    }
}