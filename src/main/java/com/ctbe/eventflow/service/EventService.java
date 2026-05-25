package com.ctbe.eventflow.service;
import com.ctbe.eventflow.dto.request.*;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.EventMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
@Service @RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    @Transactional(readOnly=true)
    public Page<EventDTO> listPublished(Pageable pageable) { return eventRepository.findByStatus(EventStatus.PUBLISHED,pageable).map(eventMapper::toDTO); }
    @Transactional(readOnly=true)
    public EventDTO getById(Long id) { return eventMapper.toDTO(findOrThrow(id)); }
    @Transactional
    public EventDTO create(CreateEventRequest req) {
        User organizer=currentUser();
        Event event=Event.builder().title(req.getTitle()).description(req.getDescription()).location(req.getLocation())
            .dateTime(req.getDateTime()).capacity(req.getCapacity())
            .status(req.getStatus()!=null?req.getStatus():EventStatus.DRAFT).createdBy(organizer).build();
        return eventMapper.toDTO(eventRepository.save(event));
    }
    @Transactional
    public EventDTO update(Long id, UpdateEventRequest req) {
        Event event=findOrThrow(id); User current=currentUser();
        if (!event.getCreatedBy().getId().equals(current.getId())&&current.getRole()!=UserRole.STAFF)
            throw new ForbiddenException("You are not the organizer of this event");
        if (req.getTitle()!=null) event.setTitle(req.getTitle());
        if (req.getDescription()!=null) event.setDescription(req.getDescription());
        if (req.getLocation()!=null) event.setLocation(req.getLocation());
        if (req.getDateTime()!=null) event.setDateTime(req.getDateTime());
        if (req.getCapacity()!=null) event.setCapacity(req.getCapacity());
        if (req.getStatus()!=null) event.setStatus(req.getStatus());
        return eventMapper.toDTO(eventRepository.save(event));
    }
    @Transactional
    public void delete(Long id) {
        Event event=findOrThrow(id); User current=currentUser();
        if (!event.getCreatedBy().getId().equals(current.getId())&&current.getRole()!=UserRole.STAFF)
            throw new ForbiddenException("You are not the organizer of this event");
        eventRepository.delete(event);
    }
    @Transactional(readOnly=true)
    public List<EventDTO> search(String keyword,String location,EventStatus status,LocalDateTime from,LocalDateTime to) {
        return eventRepository.search(keyword,location,status,from,to).stream().map(eventMapper::toDTO).toList();
    }
    private Event findOrThrow(Long id) { return eventRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Event not found: "+id)); }
    private User currentUser() {
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));
    }
}
