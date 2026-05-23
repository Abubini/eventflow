package com.ctbe.eventflow.repository;
import com.ctbe.eventflow.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RegistrationRepository extends JpaRepository<Registration,Long> {
    Optional<Registration> findByUserAndEvent(User user, Event event);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    long countByEventAndStatus(Event event, RegStatus status);
    Page<Registration> findByEvent(Event event, Pageable pageable);
    Page<Registration> findByUser(User user, Pageable pageable);
}
