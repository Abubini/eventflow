package com.ctbe.eventflow.dto.request;

import com.ctbe.eventflow.model.EventStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Used by STAFF to create an event and assign it to a specific organizer.
 * Identical to CreateEventRequest but adds a mandatory organizerId.
 */
@Data
public class CreateEventForOrganizerRequest {

    @NotNull(message = "organizerId is required — specify which organizer owns this event")
    private Long organizerId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 300)
    private String location;

    @NotNull(message = "Date and time are required")
    @Future(message = "Event date must be in the future")
    private LocalDateTime dateTime;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private EventStatus status = EventStatus.DRAFT;
}