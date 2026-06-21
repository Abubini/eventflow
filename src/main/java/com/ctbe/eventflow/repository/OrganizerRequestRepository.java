package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.OrganizerRequest;
import com.ctbe.eventflow.model.RequestStatus;
import com.ctbe.eventflow.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizerRequestRepository extends JpaRepository<OrganizerRequest, Long> {

    Page<OrganizerRequest> findByStatus(RequestStatus status, Pageable pageable);

    Page<OrganizerRequest> findByUser(User user, Pageable pageable);

    /** Prevent duplicate pending requests from the same user. */
    boolean existsByUserAndStatus(User user, RequestStatus status);

    Optional<OrganizerRequest> findByIdAndUser(Long id, User user);
}