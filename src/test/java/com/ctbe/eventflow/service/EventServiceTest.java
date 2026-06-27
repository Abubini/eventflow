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
import org.springframework.security.core.context.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock UserRepository  userRepository;
    @Mock EventMapper     eventMapper;

    @InjectMocks EventService eventService;

    private User organizer;
    private User otherOrganizer;
    private User staffUser;
    private Event event;
    private EventDTO eventDTO;

    @BeforeEach
    void setUp() {
        organizer      = User.builder().id(1L).email("org@test.com").role(UserRole.ORGANIZER).name("Org").build();
        otherOrganizer = User.builder().id(2L).email("other@test.com").role(UserRole.ORGANIZER).name("Other").build();
        staffUser      = User.builder().id(3L).email("staff@test.com").role(UserRole.STAFF).name("Staff").build();

        event = Event.builder()
                .id(1L).title("Test Event").description("A great event")
                .location("Addis Ababa").dateTime(LocalDateTime.now().plusDays(5))
                .capacity(100).status(EventStatus.DRAFT).createdBy(organizer)
                .build();

        eventDTO = EventDTO.builder().id(1L).title("Test Event")
                .location("Addis Ababa").status(EventStatus.DRAFT).build();
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
    //  listPublished
    // ════════════════════════════════════════════════════

    @Test
    void listPublished_returnsOnlyPublishedEvents() {
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        Page<EventDTO> result = eventService.listPublished(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findByStatus(eq(EventStatus.PUBLISHED), any());
    }

    @Test
    void listPublished_emptyDatabase_returnsEmptyPage() {
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any()))
                .thenReturn(Page.empty());

        assertThat(eventService.listPublished(PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void listPublished_respectsPaginationParams() {
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any()))
                .thenReturn(Page.empty());

        eventService.listPublished(PageRequest.of(2, 5));

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findByStatus(any(), cap.capture());
        assertThat(cap.getValue().getPageNumber()).isEqualTo(2);
        assertThat(cap.getValue().getPageSize()).isEqualTo(5);
    }

    // ════════════════════════════════════════════════════
    //  getById
    // ════════════════════════════════════════════════════

    @Test
    void getById_existingId_returnsDTO() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        assertThat(eventService.getById(1L).getTitle()).isEqualTo("Test Event");
    }

    @Test
    void getById_nonExistentId_throwsResourceNotFoundException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getById_zeroId_throwsResourceNotFoundException() {
        when(eventRepository.findById(0L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(0L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_negativeId_throwsResourceNotFoundException() {
        when(eventRepository.findById(-1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(-1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ════════════════════════════════════════════════════
    //  create
    // ════════════════════════════════════════════════════

    @Test
    void create_validRequest_savesAndReturnsDTO() {
        mockSecurityAs(organizer);
        CreateEventRequest req = makeCreateRequest("New Event", EventStatus.DRAFT);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        assertThat(eventService.create(req)).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void create_nullStatus_defaultsToDraft() {
        mockSecurityAs(organizer);
        CreateEventRequest req = makeCreateRequest("Event", null);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.create(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void create_withPublishedStatus_savesAsPublished() {
        mockSecurityAs(organizer);
        CreateEventRequest req = makeCreateRequest("Event", EventStatus.PUBLISHED);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.create(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void create_setsCreatedByToCurrentUser() {
        mockSecurityAs(organizer);
        CreateEventRequest req = makeCreateRequest("Event", EventStatus.DRAFT);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.create(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getCreatedBy().getId()).isEqualTo(organizer.getId());
    }

    @Test
    void create_withNullCapacity_savesWithNullCapacity() {
        mockSecurityAs(organizer);
        CreateEventRequest req = makeCreateRequest("Event", EventStatus.DRAFT);
        req.setCapacity(null);
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.create(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getCapacity()).isNull();
    }

    @Test
    void create_withDescription_savesDescription() {
        mockSecurityAs(organizer);
        CreateEventRequest req = makeCreateRequest("Event", EventStatus.DRAFT);
        req.setDescription("A detailed description");
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        eventService.create(req);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getDescription()).isEqualTo("A detailed description");
    }

    // ════════════════════════════════════════════════════
    //  update
    // ════════════════════════════════════════════════════


    @Test
    void update_byStaff_onAnotherOrganizersEvent_succeeds() {
        mockSecurityAs(staffUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Staff Updated");

        assertThatCode(() -> eventService.update(1L, req)).doesNotThrowAnyException();
    }

    @Test
    void update_byOtherOrganizer_throwsForbidden() {
        mockSecurityAs(otherOrganizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.update(1L, new UpdateEventRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("not the organizer");
    }

    @Test
    void update_allNullFields_changesNothing() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        String originalTitle    = event.getTitle();
        String originalLocation = event.getLocation();

        eventService.update(1L, new UpdateEventRequest()); // all nulls

        assertThat(event.getTitle()).isEqualTo(originalTitle);
        assertThat(event.getLocation()).isEqualTo(originalLocation);
    }

    @Test
    void update_onlyTitleProvided_onlyTitleChanges() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        String originalLocation = event.getLocation();
        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Only Title Changed");

        eventService.update(1L, req);

        assertThat(event.getTitle()).isEqualTo("Only Title Changed");
        assertThat(event.getLocation()).isEqualTo(originalLocation);
    }

    @Test
    void update_eventNotFound_throwsResourceNotFound() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.update(99L, new UpdateEventRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_verifyRepositorySaveIsCalled() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Saved");
        eventService.update(1L, req);

        verify(eventRepository, times(1)).save(event);
    }

    // ════════════════════════════════════════════════════
    //  delete
    // ════════════════════════════════════════════════════

    @Test
    void delete_byOwner_deletesEvent() {
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
    void delete_byOtherOrganizer_throwsForbiddenAndDoesNotDelete() {
        mockSecurityAs(otherOrganizer);
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
        verify(eventRepository, never()).delete(any());
    }

    // ════════════════════════════════════════════════════
    //  search
    // ════════════════════════════════════════════════════

    @Test
    void search_byKeyword_returnsMatchingEvents() {
        when(eventRepository.search("Tech", null, null, null, null))
                .thenReturn(List.of(event));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        assertThat(eventService.search("Tech", null, null, null, null)).hasSize(1);
    }

    @Test
    void search_allNullParams_returnsAllEvents() {
        when(eventRepository.search(null, null, null, null, null)).thenReturn(List.of(event));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);

        assertThat(eventService.search(null, null, null, null, null)).hasSize(1);
    }

    @Test
    void search_noMatches_returnsEmptyList() {
        when(eventRepository.search(any(), any(), any(), any(), any())).thenReturn(List.of());

        assertThat(eventService.search("XYZ", null, null, null, null)).isEmpty();
    }


    @Test
    void search_byDateRange_passesParamsToRepository() {
        LocalDateTime from = LocalDateTime.now().plusDays(1);
        LocalDateTime to   = LocalDateTime.now().plusDays(10);
        when(eventRepository.search(null, null, null, from, to)).thenReturn(List.of());

        eventService.search(null, null, null, from, to);

        verify(eventRepository).search(null, null, null, from, to);
    }

    @Test
    void search_multipleResults_allMapped() {
        Event event2 = Event.builder().id(2L).title("Event 2").location("Nairobi")
                .dateTime(LocalDateTime.now().plusDays(3)).status(EventStatus.PUBLISHED)
                .createdBy(organizer).build();
        EventDTO dto2 = EventDTO.builder().id(2L).title("Event 2").build();
        when(eventRepository.search(any(), any(), any(), any(), any()))
                .thenReturn(List.of(event, event2));
        when(eventMapper.toDTO(event)).thenReturn(eventDTO);
        when(eventMapper.toDTO(event2)).thenReturn(dto2);

        assertThat(eventService.search(null, null, null, null, null)).hasSize(2);
    }

    // ── helpers ───────────────────────────────────────────────

    private CreateEventRequest makeCreateRequest(String title, EventStatus status) {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle(title);
        req.setLocation("Addis Ababa");
        req.setDateTime(LocalDateTime.now().plusDays(10));
        req.setCapacity(50);
        req.setStatus(status);
        return req;
    }
}