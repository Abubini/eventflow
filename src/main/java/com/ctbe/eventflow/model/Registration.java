package com.ctbe.eventflow.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
/**
 * Registration entity representing user registrations for events.
 */
@Entity @Table(name="registrations",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","event_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Registration {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="event_id",nullable=false) private Event event;
    @CreationTimestamp @Column(name="registered_at",nullable=false,updatable=false) private LocalDateTime registeredAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegStatus status = RegStatus.CONFIRMED;
}
