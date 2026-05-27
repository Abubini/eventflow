package com.ctbe.eventflow.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data public class CreateScheduleRequest {
    @NotBlank @Size(max=200) private String sessionTitle;
    @Size(max=1000) private String description;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
}
