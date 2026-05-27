package com.ctbe.eventflow.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class ScheduleDTO {
    private Long id; private Long eventId; private String sessionTitle;
    private String description; private LocalDateTime startTime; private LocalDateTime endTime; private LocalDateTime createdAt;
}
