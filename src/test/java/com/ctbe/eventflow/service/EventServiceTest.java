// src/test/java/com/ctbe/eventflow/service/EventServiceTest.java
package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.CreateEventRequest;
import com.ctbe.eventflow.dto.request.UpdateEventRequest;
import com.ctbe.eventflow.dto.response.EventDTO;
import com.ctbe.eventflow.exception.ForbiddenException;
import com.ctbe.eventflow.exception.ResourceNotFoundException;
import com.ctbe.eventflow.mapper.EventMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.EventRepository;
import com.ctbe.eventflow.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock EventMapper eventMapper;

    @InjectMocks EventService eventService;

    private User organizer;
    private User otherUser;
    private User staffUser;
    private Event event;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).email("org@example.com").role(UserRole.ORGANIZER).build();
        otherUser = User.builder().id(2L).email("other@example.com").role(UserRole.ORGANIZER).build();
        staffUser = User.builder().id(3L).email("staff@example.com").role(UserRole.STAFF).build();

        event = Event.builder()
                .id(1L).title("Test Event").location("Addis Ababa")
                .dateTime(LocalDateTime.now().plusDays(5))
                .status(EventStatus.DRAFT).createdBy(organizer)
                .build();

        eventDTO = EventDTO.builder().id(1L).title("Test Event").build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityAs(User user) {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        lenient().when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ── listPublished ─────────────────────────────────────────

    @Test
    void listPublished_returnsPageOfPublishedEvents() {
        Page<Event> page = new PageImpl<>(List.of(event));
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class))).thenReturn(page);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        Page<EventDTO> result = eventService.listPublished(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Event");
    }

    // ── getById ───────────────────────────────────────────────

    @Test
    void getById_existingId_returnsDTO() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        EventDTO result = eventService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Test Event");
    }

    @Test
    void getById_nonExistentId_throwsResourceNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────

    @Test
    void create_validRequest_savesAndReturnsDTO() {
        mockSecurityAs(organizer);

        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("New Event");
        req.setLocation("Addis Ababa");
        req.setDateTime(LocalDateTime.now().plusDays(10));

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        EventDTO result = eventService.create(req);

        assertThat(result).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_withNullStatus_defaultsToDraft() {
        mockSecurityAs(organizer);

        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("New Event");
        req.setLocation("Location");
        req.setDateTime(LocalDateTime.now().plusDays(10));
        req.setStatus(null);

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.create(req);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    // ── update ────────────────────────────────────────────────

    @Test
    void update_byOrganizer_updatesFields() {
        mockSecurityAs(organizer);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Updated Title");
        req.setStatus(EventStatus.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(EventDTO.builder().title("Updated Title").build());

        EventDTO result = eventService.update(1L, req);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(eventRepository).save(event);
    }

    @Test
    void update_byStaff_succeeds() {
        mockSecurityAs(staffUser);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Staff Update");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        assertThatCode(() -> eventService.update(1L, req)).doesNotThrowAnyException();
    }

    @Test
    void update_byOtherUser_throwsForbidden() {
        mockSecurityAs(otherUser);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Stolen Update");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.update(1L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_onlyNonNullFieldsAreApplied() {
        mockSecurityAs(organizer);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("New Title");
        // location, dateTime, capacity, status all null

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.update(1L, req);

        // location should be unchanged
        assertThat(event.getLocation()).isEqualTo("Addis Ababa");
        assertThat(event.getTitle()).isEqualTo("New Title");
    }

    // ── delete ────────────────────────────────────────────────

    @Test
    void delete_byOrganizer_deletesEvent() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        eventService.delete(1L);

        verify(eventRepository).delete(event);
    }

    @Test
    void delete_byStaff_deletesEvent() {
        mockSecurityAs(staffUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatCode(() -> eventService.delete(1L)).doesNotThrowAnyException();
        verify(eventRepository).delete(event);
    }

    @Test
    void delete_byOtherUser_throwsForbidden() {
        mockSecurityAs(otherUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.delete(1L))
                .isInstanceOf(ForbiddenException.class);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void delete_nonExistentEvent_throwsResourceNotFound() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── search ────────────────────────────────────────────────

    @Test
    void search_byKeyword_returnsMatchingEvents() {
        when(eventRepository.search("Tech", null, null, null, null))
                .thenReturn(List.of(event));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        List<EventDTO> result = eventService.search("Tech", null, null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void search_noResults_returnsEmptyList() {
        when(eventRepository.search(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        List<EventDTO> result = eventService.search("NonExistent", null, null, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void search_allNullParams_returnsAllEvents() {
        when(eventRepository.search(null, null, null, null, null))
                .thenReturn(List.of(event));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        List<EventDTO> result = eventService.search(null, null, null, null, null);

        assertThat(result).hasSize(1);
    }
}