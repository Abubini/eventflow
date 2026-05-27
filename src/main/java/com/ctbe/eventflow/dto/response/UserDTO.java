package com.ctbe.eventflow.dto.response;
import com.ctbe.eventflow.model.UserRole;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class UserDTO {
    private Long id; private String name; private String email;
    private UserRole role; private boolean active; private LocalDateTime createdAt;
}
