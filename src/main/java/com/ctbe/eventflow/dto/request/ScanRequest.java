package com.ctbe.eventflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ScanRequest {
    @NotNull(message = "Ticket code is required")
    private UUID ticketCode;
}
