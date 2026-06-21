package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByUserAndEvent(User user, Event event);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    long countByEventAndStatus(Event event, RegStatus status);

    Page<Registration> findByEvent(Event event, Pageable pageable);

    Page<Registration> findByUser(User user, Pageable pageable);

    /** For the attendee's "my registrations" list — ordered newest first. */
    Page<Registration> findByUserOrderByRegisteredAtDesc(User user, Pageable pageable);

    /** Ticket lookup by QR code payload. */
    Optional<Registration> findByTicketCode(UUID ticketCode);

    /** Total confirmed seats (sum of attendeeCount) for an event. */
    @Query("SELECT COALESCE(SUM(r.attendeeCount), 0) FROM Registration r " +
            "WHERE r.event = :event AND r.status = :status")
    long sumAttendeeCountByEventAndStatus(@Param("event") Event event,
                                          @Param("status") RegStatus status);
}