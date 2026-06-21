package com.ctbe.eventflow.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class OrganizerRequestForm {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 30, message = "Phone number too long")
    @Pattern(regexp = "^[+\\d\\s\\-()]{6,30}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Message is required")
    @Size(min = 20, max = 2000,
            message = "Message must be between 20 and 2000 characters")
    private String message;
}