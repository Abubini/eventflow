package com.ctbe.eventflow.mapper;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.model.Event;
import com.ctbe.eventflow.model.RegStatus;
import com.ctbe.eventflow.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor public class EventMapper {
    private final UserMapper userMapper;
    private final RegistrationRepository registrationRepository;
    public EventDTO toDTO(Event event) {
        long count = registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED);
        return EventDTO.builder().id(event.getId()).title(event.getTitle())
            .description(event.getDescription()).location(event.getLocation())
            .dateTime(event.getDateTime()).capacity(event.getCapacity())
            .registeredCount(count).status(event.getStatus())
            .createdBy(userMapper.toDTO(event.getCreatedBy()))
            .createdAt(event.getCreatedAt()).build();
    }
}
