package com.ctbe.eventflow.mapper;

import com.ctbe.eventflow.dto.response.RegistrationDTO;
import com.ctbe.eventflow.dto.response.TicketDTO;
import com.ctbe.eventflow.dto.response.WaitlistDTO;
import com.ctbe.eventflow.model.Registration;
import com.ctbe.eventflow.model.WaitlistEntry;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {

    public RegistrationDTO toDTO(Registration r) {
        return RegistrationDTO.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .eventTitle(r.getEvent().getTitle())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .registeredAt(r.getRegisteredAt())
                .status(r.getStatus())
                .ticketCode(r.getTicketCode())
                .scanned(r.isScanned())
                .scannedAt(r.getScannedAt())
                .attendeeCount(r.getAttendeeCount())
                .build();
    }

    public TicketDTO toTicketDTO(Registration r, String qrCodeBase64) {
        return TicketDTO.builder()
                .registrationId(r.getId())
                .ticketCode(r.getTicketCode())
                .eventId(r.getEvent().getId())
                .eventTitle(r.getEvent().getTitle())
                .eventLocation(r.getEvent().getLocation())
                .eventDateTime(r.getEvent().getDateTime())
                .attendeeId(r.getUser().getId())
                .attendeeName(r.getUser().getName())
                .attendeeEmail(r.getUser().getEmail())
                .registeredAt(r.getRegisteredAt())
                .status(r.getStatus())
                .scanned(r.isScanned())
                .scannedAt(r.getScannedAt())
                .attendeeCount(r.getAttendeeCount())
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    public WaitlistDTO toWaitlistDTO(WaitlistEntry w) {
        return WaitlistDTO.builder()
                .id(w.getId())
                .eventId(w.getEvent().getId())
                .eventTitle(w.getEvent().getTitle())
                .userId(w.getUser().getId())
                .userName(w.getUser().getName())
                .createdAt(w.getCreatedAt())
                .notified(w.isNotified())
                .build();
    }
}