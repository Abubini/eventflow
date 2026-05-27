package com.ctbe.eventflow.dto.request;
import com.ctbe.eventflow.model.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data public class RoleUpdateRequest {
    @NotNull private UserRole role;
}
