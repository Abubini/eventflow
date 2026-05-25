package com.ctbe.eventflow.mapper;
import com.ctbe.eventflow.dto.response.ScheduleDTO;
import com.ctbe.eventflow.model.Schedule;
import org.springframework.stereotype.Component;
@Component public class ScheduleMapper {
    public ScheduleDTO toDTO(Schedule s) {
        return ScheduleDTO.builder().id(s.getId()).eventId(s.getEvent().getId())
            .sessionTitle(s.getSessionTitle()).description(s.getDescription())
            .startTime(s.getStartTime()).endTime(s.getEndTime()).createdAt(s.getCreatedAt()).build();
    }
}
