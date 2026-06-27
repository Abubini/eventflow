package com.ctbe.eventflow.dto.response;

import com.ctbe.eventflow.model.RegStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full ticket response including the QR code as a base64-encoded PNG.
 */
@Data
@Builder
public class TicketDTO {
    private Long registrationId;
    private UUID ticketCode;

    // Event info
    private Long eventId;
    private String eventTitle;
    private String eventLocation;
    private LocalDateTime eventDateTime;

    // Attendee info
    private Long attendeeId;
    private String attendeeName;
    private String attendeeEmail;

    private LocalDateTime registeredAt;
    private RegStatus status;
    private boolean scanned;
    private LocalDateTime scannedAt;

    /**
     * Base64-encoded PNG of the QR code.
     * Frontend can use as: <img src="data:image/png;base64,{qrCodeBase64}" />
     */
    private String qrCodeBase64;
    private int attendeeCount;
}