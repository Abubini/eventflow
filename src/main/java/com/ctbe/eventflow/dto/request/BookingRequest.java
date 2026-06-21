package com.ctbe.eventflow.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class BookingRequest {

    /**
     * Number of attendees this booking covers.
     * Defaults to 1; max 20 per booking to prevent abuse.
     */
    @Min(value = 1, message = "Must book at least 1 attendee")
    @Max(value = 20, message = "Cannot book more than 20 attendees at once")
    private int attendeeCount = 1;
}