package com.ctbe.eventflow.mapper;
import com.ctbe.eventflow.dto.response.RegistrationDTO;
import com.ctbe.eventflow.model.Registration;
import org.springframework.stereotype.Component;
@Component public class RegistrationMapper {
    public RegistrationDTO toDTO(Registration r) {
        return RegistrationDTO.builder().id(r.getId()).eventId(r.getEvent().getId())
            .eventTitle(r.getEvent().getTitle()).userId(r.getUser().getId())
            .userName(r.getUser().getName()).registeredAt(r.getRegisteredAt())
            .status(r.getStatus()).build();
    }
}
