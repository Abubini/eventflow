package com.ctbe.eventflow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registration entity representing user registrations for events.
 */
@Entity
@Table(name = "registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegStatus status = RegStatus.CONFIRMED;

    /** Unique ticket identifier — used as the QR code payload. */
    @Column(name = "ticket_code", nullable = false, unique = true, updatable = false)
    private UUID ticketCode;

    /** True once an organizer has scanned this ticket at entry. */
    @Column(name = "scanned", nullable = false)
    @Builder.Default
    private boolean scanned = false;

    /** When the ticket was scanned. */
    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @PrePersist
    private void prePersist() {
        if (ticketCode == null) {
            ticketCode = UUID.randomUUID();
        }
    }

    @Column(name = "attendee_count", nullable = false)
    @Builder.Default
    private int attendeeCount = 1;
}