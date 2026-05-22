package com.ctbe.eventflow.model;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
@Entity @Table(name="schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Schedule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="event_id",nullable=false) private Event event;
    @Column(name="session_title",nullable=false,length=200) private String sessionTitle;
    @Column(length=1000) private String description;
    @Column(name="start_time",nullable=false) private LocalDateTime startTime;
    @Column(name="end_time",nullable=false) private LocalDateTime endTime;
    @CreationTimestamp @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
}
