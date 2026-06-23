package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.CreateEventForOrganizerRequest;
import com.ctbe.eventflow.dto.request.CreateEventRequest;
import com.ctbe.eventflow.dto.request.UpdateEventRequest;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.EventMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository        eventRepository;
    private final UserRepository         userRepository;
    private final RegistrationRepository registrationRepository;
    private final EventMapper            eventMapper;
    private final EmailService           emailService;
    private final RegistrationService    registrationService;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' HH:mm");

    // ── List published ────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventDTO> listPublished(Pageable pageable) {
        return eventRepository.findByStatus(EventStatus.PUBLISHED, pageable)
                .map(eventMapper::toDTO);
    }

    // ── Get by id ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EventDTO getById(Long id) {
        return eventMapper.toDTO(findOrThrow(id));
    }

    // ── Create ────────────────────────────────────────────────

    @Transactional
    public EventDTO create(CreateEventRequest req) {
        User organizer = currentUser();
        Event event = Event.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .location(req.getLocation())
                .dateTime(req.getDateTime())
                .capacity(req.getCapacity())
                .status(req.getStatus() != null ? req.getStatus() : EventStatus.DRAFT)
                .createdBy(organizer)
                .build();
        return eventMapper.toDTO(eventRepository.save(event));
    }

    // ── Create on behalf of an organizer (staff only) ─────────

    /**
     * Staff picks which organizer will own the event.
     * The target user must have the ORGANIZER role.
     */
    @Transactional
    public EventDTO createForOrganizer(CreateEventForOrganizerRequest req) {
        // Resolve and validate the target organizer
        User organizer = userRepository.findById(req.getOrganizerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + req.getOrganizerId()));

        if (organizer.getRole() != UserRole.ORGANIZER)
            throw new BadRequestException(
                    "User " + req.getOrganizerId() +
                            " is not an organizer (role: " + organizer.getRole() + "). " +
                            "Only organizers can own events.");

        Event event = Event.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .location(req.getLocation())
                .dateTime(req.getDateTime())
                .capacity(req.getCapacity())
                .status(req.getStatus() != null ? req.getStatus() : EventStatus.DRAFT)
                .createdBy(organizer)        // ← owned by the chosen organizer, not the staff member
                .build();

        return eventMapper.toDTO(eventRepository.save(event));
    }

    // ── Update ────────────────────────────────────────────────

    @Transactional
    public EventDTO update(Long id, UpdateEventRequest req) {
        Event event   = findOrThrow(id);
        User  current = currentUser();

        if (!event.getCreatedBy().getId().equals(current.getId())
                && current.getRole() != UserRole.STAFF)
            throw new ForbiddenException("You are not the organizer of this event");

        // ── Detect date change before mutating ────────────────
        LocalDateTime oldDateTime = event.getDateTime();
        boolean dateChanged = req.getDateTime() != null
                && !req.getDateTime().isEqual(oldDateTime);

        // ── Detect capacity increase before mutating ──────────
        Integer oldCapacity = event.getCapacity();
        boolean capacityIncreased = req.getCapacity() != null
                && (oldCapacity == null || req.getCapacity() > oldCapacity);

        // Apply changes
        if (req.getTitle()       != null) event.setTitle(req.getTitle());
        if (req.getDescription() != null) event.setDescription(req.getDescription());
        if (req.getLocation()    != null) event.setLocation(req.getLocation());
        if (req.getDateTime()    != null) event.setDateTime(req.getDateTime());
        if (req.getCapacity()    != null) event.setCapacity(req.getCapacity());
        if (req.getStatus()      != null) event.setStatus(req.getStatus());

        Event saved = eventRepository.save(event);

        // ── Send reschedule emails to all registered attendees ─
        if (dateChanged) {
            String oldFormatted = oldDateTime.format(FMT);
            registrationRepository.findByEvent(saved, Pageable.unpaged())
                    .forEach(reg -> emailService.sendRescheduledNotification(
                            reg.getUser(), saved, oldFormatted));
        }

        // ── Notify waitlist if new capacity freed slots ────────
        if (capacityIncreased) {
            long used = registrationRepository
                    .sumAttendeeCountByEventAndStatus(saved, RegStatus.CONFIRMED);
            int newlyAvailable = (int) (saved.getCapacity() - used);
            if (newlyAvailable > 0) {
                registrationService.notifyWaitlist(saved, newlyAvailable);
            }
        }

        return eventMapper.toDTO(saved);
    }

    // ── Delete ────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        Event event   = findOrThrow(id);
        User  current = currentUser();
        if (!event.getCreatedBy().getId().equals(current.getId())
                && current.getRole() != UserRole.STAFF)
            throw new ForbiddenException("You are not the organizer of this event");
        eventRepository.delete(event);
    }

    // ── Search ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EventDTO> search(String keyword, String location,
                                 EventStatus status,
                                 LocalDateTime from, LocalDateTime to) {
        return eventRepository.search(keyword, location, status, from, to)
                .stream().map(eventMapper::toDTO).toList();
    }

    // ── My events (organizer) ─────────────────────────────────

    /**
     * Returns all events created by the currently authenticated organizer,
     * ordered by event date descending (upcoming first).
     * STAFF can also call this to see events they created.
     */
    @Transactional(readOnly = true)
    public Page<EventDTO> getMyEvents(Pageable pageable) {
        User current = currentUser();
        return eventRepository
                .findByCreatedByOrderByDateTimeDesc(current, pageable)
                .map(eventMapper::toDTO);
    }

    // ── Helpers ───────────────────────────────────────────────

    private Event findOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}