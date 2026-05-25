package com.ctbe.eventflow.service;
import com.ctbe.eventflow.dto.request.CreateScheduleRequest;
import com.ctbe.eventflow.dto.response.ScheduleDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.ScheduleMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ScheduleMapper scheduleMapper;
    @Transactional(readOnly=true)
    public List<ScheduleDTO> getSchedules(Long eventId) { return scheduleRepository.findByEventId(eventId).stream().map(scheduleMapper::toDTO).toList(); }
    @Transactional
    public ScheduleDTO addSession(Long eventId,CreateScheduleRequest req) {
        Event event=eventRepository.findById(eventId).orElseThrow(()->new ResourceNotFoundException("Event not found: "+eventId));
        User current=currentUser();
        if (!event.getCreatedBy().getId().equals(current.getId())&&current.getRole()!=UserRole.STAFF) throw new ForbiddenException("Only the organizer can add sessions");
        if (!req.getEndTime().isAfter(req.getStartTime())) throw new BadRequestException("End time must be after start time");
        Schedule s=Schedule.builder().event(event).sessionTitle(req.getSessionTitle()).description(req.getDescription()).startTime(req.getStartTime()).endTime(req.getEndTime()).build();
        return scheduleMapper.toDTO(scheduleRepository.save(s));
    }
    private User currentUser() {
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));
    }
}
