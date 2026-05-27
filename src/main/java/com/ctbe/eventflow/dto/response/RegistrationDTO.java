package com.ctbe.eventflow.dto.response;
import com.ctbe.eventflow.model.RegStatus;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class RegistrationDTO {
    private Long id; private Long eventId; private String eventTitle;
    private Long userId; private String userName; private LocalDateTime registeredAt; private RegStatus status;
}
