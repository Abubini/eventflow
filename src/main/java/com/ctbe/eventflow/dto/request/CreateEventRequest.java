package com.ctbe.eventflow.dto.request;
import com.ctbe.eventflow.model.EventStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class CreateEventRequest {
    @NotBlank @Size(max=200) private String title;
    @Size(max=2000) private String description;
    @NotBlank @Size(max=300) private String location;
    @NotNull @Future private LocalDateTime dateTime;
    @Min(1) private Integer capacity;
    private EventStatus status=EventStatus.DRAFT;
}
