package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.CreateEventForOrganizerRequest;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.EventMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtendedEventServiceTest {

    @Mock EventRepository        eventRepository;
    @Mock UserRepository         userRepository;
    @Mock RegistrationRepository registrationRepository;
    @Mock EventMapper            eventMapper;

    @InjectMocks EventService eventService;

    private User organizer;
    private User staffUser;
    private Event event;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).email("org@test.com")
                .role(UserRole.ORGANIZER).name("Org").build();
        staffUser = User.builder().id(3L).email("staff@test.com")
                .role(UserRole.STAFF).name("Staff").build();
        event = Event.builder().id(1L).title("Event").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(5))
                .status(EventStatus.DRAFT).createdBy(organizer).build();
        eventDTO = EventDTO.builder().id(1L).title("Event").build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private void mockSecurityAs(User user) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ════════════════════════════════════════════════════
    //  getMyEvents
    // ════════════════════════════════════════════════════

    @Test
    void getMyEvents_returnsOrganizerOwnEvents() {
        mockSecurityAs(organizer);
        when(eventRepository.findByCreatedByOrderByDateTimeDesc(eq(organizer), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        Page<EventDTO> result = eventService.getMyEvents(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Event");
    }

    @Test
    void getMyEvents_noEvents_returnsEmptyPage() {
        mockSecurityAs(organizer);
        when(eventRepository.findByCreatedByOrderByDateTimeDesc(eq(organizer), any()))
                .thenReturn(Page.empty());

        assertThat(eventService.getMyEvents(PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void getMyEvents_staffSeesTheirCreatedEvents() {
        mockSecurityAs(staffUser);
        when(eventRepository.findByCreatedByOrderByDateTimeDesc(eq(staffUser), any()))
                .thenReturn(Page.empty());

        assertThatCode(() -> eventService.getMyEvents(PageRequest.of(0, 10)))
                .doesNotThrowAnyException();
        verify(eventRepository).findByCreatedByOrderByDateTimeDesc(eq(staffUser), any());
    }

    @Test
    void getMyEvents_multipleEvents_allReturned() {
        mockSecurityAs(organizer);
        Event event2 = Event.builder().id(2L).title("Event 2").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(10))
                .status(EventStatus.PUBLISHED).createdBy(organizer).build();
        EventDTO dto2 = EventDTO.builder().id(2L).title("Event 2").build();
        when(eventRepository.findByCreatedByOrderByDateTimeDesc(eq(organizer), any()))
                .thenReturn(new PageImpl<>(List.of(event, event2)));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);
        when(eventMapper.toDTO(event2)).thenReturn(dto2);

        assertThat(eventService.getMyEvents(PageRequest.of(0, 10)).getContent()).hasSize(2);
    }

    // ════════════════════════════════════════════════════
    //  createForOrganizer
    // ════════════════════════════════════════════════════

    @Test
    void createForOrganizer_validOrganizer_eventOwnedByOrganizer() {
        mockSecurityAs(staffUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        CreateEventForOrganizerRequest req = makeRequest(1L);
        eventService.createForOrganizer(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getCreatedBy().getId()).isEqualTo(organizer.getId());
    }

    @Test
    void createForOrganizer_validOrganizer_returnsDTO() {
        mockSecurityAs(staffUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        EventDTO result = eventService.createForOrganizer(makeRequest(1L));

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Event");
    }

    @Test
    void createForOrganizer_targetIsAttendee_throwsBadRequest() {
        mockSecurityAs(staffUser);
        User attendee = User.builder().id(5L).email("att@test.com")
                .role(UserRole.ATTENDEE).name("Att").build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(attendee));

        assertThatThrownBy(() -> eventService.createForOrganizer(makeRequest(5L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not an organizer");
    }

    @Test
    void createForOrganizer_targetIsStaff_throwsBadRequest() {
        mockSecurityAs(staffUser);
        User anotherStaff = User.builder().id(9L).email("s2@test.com")
                .role(UserRole.STAFF).name("S2").build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(anotherStaff));

        assertThatThrownBy(() -> eventService.createForOrganizer(makeRequest(9L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not an organizer");
    }

    @Test
    void createForOrganizer_targetNotFound_throwsResourceNotFound() {
        mockSecurityAs(staffUser);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createForOrganizer(makeRequest(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createForOrganizer_nullStatus_defaultsToDraft() {
        mockSecurityAs(staffUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(organizer));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        CreateEventForOrganizerRequest req = makeRequest(1L);
        req.setStatus(null);
        eventService.createForOrganizer(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    private CreateEventForOrganizerRequest makeRequest(Long organizerId) {
        CreateEventForOrganizerRequest req = new CreateEventForOrganizerRequest();
        req.setOrganizerId(organizerId);
        req.setTitle("Event");
        req.setLocation("Addis");
        req.setDateTime(LocalDateTime.now().plusDays(10));
        req.setStatus(EventStatus.DRAFT);
        return req;
    }
}