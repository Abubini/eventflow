package com.ctbe.eventflow.dto.response;
import com.ctbe.eventflow.model.EventStatus;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class EventDTO {
    private Long id; private String title; private String description;
    private String location; private LocalDateTime dateTime; private Integer capacity;
    private long registeredCount; private EventStatus status; private UserDTO createdBy; private LocalDateTime createdAt;
}
