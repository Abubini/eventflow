package com.ctbe.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result returned after an organizer scans a ticket QR code.
 */
@Data
@Builder
public class ScanResultDTO {
    private boolean valid;
    private String message;

    // Populated when valid = true
    private UUID ticketCode;
    private String attendeeName;
    private String attendeeEmail;
    private String eventTitle;
    private LocalDateTime eventDateTime;
    private LocalDateTime scannedAt;
}