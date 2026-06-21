package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.Event;
import com.ctbe.eventflow.model.User;
import com.ctbe.eventflow.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    Optional<WaitlistEntry> findByUserAndEvent(User user, Event event);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    /** All un-notified waitlist entries for an event, oldest first. */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.event = :event AND w.notified = false " +
            "ORDER BY w.createdAt ASC")
    List<WaitlistEntry> findPendingByEvent(@Param("event") Event event);

    List<WaitlistEntry> findByEvent(Event event);
}