package com.ctbe.eventflow.service;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.*;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    @Transactional
    public RegistrationDTO register(Long eventId) {
        User user=currentUser();
        Event event=eventRepository.findById(eventId).orElseThrow(()->new ResourceNotFoundException("Event not found: "+eventId));
        if (event.getStatus()!=EventStatus.PUBLISHED) throw new BadRequestException("Event is not open for registration");
        if (registrationRepository.existsByUserIdAndEventId(user.getId(),eventId)) throw new ConflictException("Already registered for this event");
        if (event.getCapacity()!=null) {
            long confirmed=registrationRepository.countByEventAndStatus(event,RegStatus.CONFIRMED);
            if (confirmed>=event.getCapacity()) throw new BadRequestException("Event is at full capacity");
        }
        Registration reg=Registration.builder().user(user).event(event).status(RegStatus.CONFIRMED).build();
        return registrationMapper.toDTO(registrationRepository.save(reg));
    }
    @Transactional
    public void cancel(Long eventId) {
        User user=currentUser();
        Event event=eventRepository.findById(eventId).orElseThrow(()->new ResourceNotFoundException("Event not found: "+eventId));
        Registration reg=registrationRepository.findByUserAndEvent(user,event).orElseThrow(()->new ResourceNotFoundException("Registration not found"));
        registrationRepository.delete(reg);
    }
    @Transactional(readOnly=true)
    public Page<UserDTO> getAttendees(Long eventId,Pageable pageable) {
        Event event=eventRepository.findById(eventId).orElseThrow(()->new ResourceNotFoundException("Event not found: "+eventId));
        User current=currentUser();
        if (!event.getCreatedBy().getId().equals(current.getId())&&current.getRole()!=UserRole.STAFF) throw new ForbiddenException("Access denied");
        return registrationRepository.findByEvent(event,pageable).map(r->userMapper.toDTO(r.getUser()));
    }
    private User currentUser() {
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));
    }
}
