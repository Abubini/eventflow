package com.ctbe.eventflow.dto.request;
import com.ctbe.eventflow.model.EventStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class UpdateEventRequest {
    @Size(max=200) private String title;
    @Size(max=2000) private String description;
    @Size(max=300) private String location;
    private LocalDateTime dateTime;
    @Min(1) private Integer capacity;
    private EventStatus status;
}
