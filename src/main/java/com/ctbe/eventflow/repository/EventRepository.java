package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.Event;
import com.ctbe.eventflow.model.EventStatus;
import com.ctbe.eventflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    List<Event> findByCreatedBy(User createdBy);

    /** Organizer's own events, newest first. */
    Page<Event> findByCreatedByOrderByDateTimeDesc(User createdBy, Pageable pageable);

    @Query("""
           SELECT e FROM Event e
           WHERE (:keyword  IS NULL OR LOWER(e.title)    LIKE LOWER(CONCAT('%', :keyword,  '%'))
                                    OR LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
             AND (:location IS NULL OR LOWER(e.location) LIKE LOWER(CONCAT('%', :location, '%')))
             AND (:status   IS NULL OR e.status = :status)
             AND (:from     IS NULL OR e.dateTime >= :from)
             AND (:to       IS NULL OR e.dateTime <= :to)
           ORDER BY e.dateTime ASC
           """)
    List<Event> search(@Param("keyword")  String keyword,
                       @Param("location") String location,
                       @Param("status")   EventStatus status,
                       @Param("from")     LocalDateTime from,
                       @Param("to")       LocalDateTime to);
}