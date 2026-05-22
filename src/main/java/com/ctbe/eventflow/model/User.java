package com.ctbe.eventflow.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
/**
 * User entity representing registered users in the system.
 * Supports three roles: ORGANIZER, ATTENDEE, STAFF.
 */
@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,unique=true,length=255) private String email;
    @Column(name="password_hash",nullable=false) private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.ATTENDEE;
    @Column(nullable=false) private boolean active=true;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @OneToMany(mappedBy="createdBy",cascade=CascadeType.ALL,fetch=FetchType.LAZY) @Builder.Default private List<Event> events=new ArrayList<>();
    @OneToMany(mappedBy="user",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @Builder.Default private List<Registration> registrations=new ArrayList<>();
}
