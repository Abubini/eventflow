package com.ctbe.eventflow.mapper;

import com.ctbe.eventflow.dto.response.OrganizerRequestDTO;
import com.ctbe.eventflow.model.OrganizerRequest;
import org.springframework.stereotype.Component;

@Component
public class OrganizerRequestMapper {

    public OrganizerRequestDTO toDTO(OrganizerRequest r) {
        return OrganizerRequestDTO.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userEmail(r.getUser().getEmail())
                .userCurrentRole(r.getUser().getRole().name())
                .name(r.getName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .message(r.getMessage())
                .status(r.getStatus())
                .reviewedByName(r.getReviewedBy() != null ? r.getReviewedBy().getName() : null)
                .reviewNote(r.getReviewNote())
                .createdAt(r.getCreatedAt())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}