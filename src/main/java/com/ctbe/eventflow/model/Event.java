package com.ctbe.eventflow.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=200) private String title;
    @Column(length=2000) private String description;
    @Column(nullable=false,length=300) private String location;
    @Column(name="date_time",nullable=false) private LocalDateTime dateTime;
    private Integer capacity;

    @Column(nullable=false, length=50)
    private EventStatus status = EventStatus.DRAFT;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by",nullable=false) private User createdBy;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @OneToMany(mappedBy="event",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @Builder.Default private List<Registration> registrations=new ArrayList<>();
    @OneToMany(mappedBy="event",cascade=CascadeType.ALL,orphanRemoval=true,fetch=FetchType.LAZY) @Builder.Default private List<Schedule> schedules=new ArrayList<>();
}