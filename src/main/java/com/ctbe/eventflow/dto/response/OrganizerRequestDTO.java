package com.ctbe.eventflow.dto.response;

import com.ctbe.eventflow.model.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizerRequestDTO {
    private Long id;

    // Submitter
    private Long userId;
    private String userEmail;       // account email
    private String userCurrentRole;

    // Form data
    private String name;
    private String email;           // contact email from form
    private String phone;
    private String message;

    // Review
    private RequestStatus status;
    private String reviewedByName;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}