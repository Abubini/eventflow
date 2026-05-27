package com.ctbe.eventflow.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data public class UpdateProfileRequest {
    @NotBlank @Size(max=100) private String name;
}
