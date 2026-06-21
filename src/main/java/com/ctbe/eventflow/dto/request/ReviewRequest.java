package com.ctbe.eventflow.dto.request;

import com.ctbe.eventflow.model.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Decision is required (APPROVED or DECLINED)")
    private RequestStatus decision;   // APPROVED or DECLINED only

    /** Optional note to include in the notification email to the user. */
    private String note;
}