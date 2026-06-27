package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.CreateScheduleRequest;
import com.ctbe.eventflow.dto.response.ScheduleDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.ScheduleMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock EventRepository    eventRepository;
    @Mock UserRepository     userRepository;
    @Mock ScheduleMapper     scheduleMapper;

    @InjectMocks ScheduleService scheduleService;

    private User organizer;
    private User otherUser;
    private User staffUser;
    private Event event;
    private final LocalDateTime BASE = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).email("org@test.com").role(UserRole.ORGANIZER).build();
        otherUser = User.builder().id(2L).email("other@test.com").role(UserRole.ORGANIZER).build();
        staffUser = User.builder().id(3L).email("staff@test.com").role(UserRole.STAFF).build();
        event     = Event.builder().id(1L).title("Conference").status(EventStatus.PUBLISHED)
                .createdBy(organizer).dateTime(BASE).build();
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

    private CreateScheduleRequest makeReq(LocalDateTime start, LocalDateTime end) {
        CreateScheduleRequest req = new CreateScheduleRequest();
        req.setSessionTitle("Keynote");
        req.setDescription("Opening keynote");
        req.setStartTime(start);
        req.setEndTime(end);
        return req;
    }

    // ════════════════════════════════════════════════════
    //  getSchedules
    // ════════════════════════════════════════════════════

    @Test
    void getSchedules_returnsAllForEvent() {
        Schedule s = Schedule.builder().id(1L).event(event)
                .sessionTitle("Keynote").startTime(BASE).endTime(BASE.plusHours(1)).build();
        ScheduleDTO dto = ScheduleDTO.builder().id(1L).sessionTitle("Keynote").build();
        when(scheduleRepository.findByEventId(1L)).thenReturn(List.of(s));
        when(scheduleMapper.toDTO(s)).thenReturn(dto);

        List<ScheduleDTO> result = scheduleService.getSchedules(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionTitle()).isEqualTo("Keynote");
    }

    @Test
    void getSchedules_noSchedules_returnsEmptyList() {
        when(scheduleRepository.findByEventId(1L)).thenReturn(List.of());

        assertThat(scheduleService.getSchedules(1L)).isEmpty();
    }

    @Test
    void getSchedules_multipleSchedules_allReturned() {
        Schedule s1 = Schedule.builder().id(1L).event(event).sessionTitle("S1")
                .startTime(BASE).endTime(BASE.plusHours(1)).build();
        Schedule s2 = Schedule.builder().id(2L).event(event).sessionTitle("S2")
                .startTime(BASE.plusHours(2)).endTime(BASE.plusHours(3)).build();
        when(scheduleRepository.findByEventId(1L)).thenReturn(List.of(s1, s2));
        when(scheduleMapper.toDTO(s1)).thenReturn(ScheduleDTO.builder().id(1L).build());
        when(scheduleMapper.toDTO(s2)).thenReturn(ScheduleDTO.builder().id(2L).build());

        assertThat(scheduleService.getSchedules(1L)).hasSize(2);
    }

    @Test
    void getSchedules_unknownEventId_returnsEmpty() {
        when(scheduleRepository.findByEventId(999L)).thenReturn(List.of());

        assertThat(scheduleService.getSchedules(999L)).isEmpty();
    }

    // ════════════════════════════════════════════════════
    //  addSession
    // ════════════════════════════════════════════════════

    @Test
    void addSession_byOrganizer_savesAndReturnsDTO() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        Schedule saved = Schedule.builder().id(5L).event(event).sessionTitle("Keynote")
                .startTime(BASE).endTime(BASE.plusHours(2)).build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(5L).sessionTitle("Keynote").build());

        ScheduleDTO result = scheduleService.addSession(1L, makeReq(BASE, BASE.plusHours(2)));

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getSessionTitle()).isEqualTo("Keynote");
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void addSession_byStaff_succeeds() {
        mockSecurityAs(staffUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        Schedule saved = Schedule.builder().id(6L).event(event).sessionTitle("Keynote")
                .startTime(BASE).endTime(BASE.plusHours(1)).build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(6L).build());

        assertThatCode(() -> scheduleService.addSession(1L, makeReq(BASE, BASE.plusHours(1))))
                .doesNotThrowAnyException();
    }

    @Test
    void addSession_byOtherOrganizer_throwsForbidden() {
        mockSecurityAs(otherUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> scheduleService.addSession(1L, makeReq(BASE, BASE.plusHours(1))))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("organizer");
    }

    @Test
    void addSession_byOtherOrganizer_neverSaves() {
        mockSecurityAs(otherUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> scheduleService.addSession(1L, makeReq(BASE, BASE.plusHours(1))));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void addSession_endTimeBeforeStartTime_throwsBadRequest() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> scheduleService.addSession(1L,
                makeReq(BASE.plusHours(3), BASE.plusHours(1)))) // end before start
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void addSession_endTimeEqualsStartTime_throwsBadRequest() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> scheduleService.addSession(1L, makeReq(BASE, BASE)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addSession_endTimeOneSecondAfterStart_succeeds() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        LocalDateTime start = BASE;
        LocalDateTime end   = BASE.plusSeconds(1);
        Schedule saved = Schedule.builder().id(7L).event(event).sessionTitle("Keynote")
                .startTime(start).endTime(end).build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(7L).build());

        assertThatCode(() -> scheduleService.addSession(1L, makeReq(start, end)))
                .doesNotThrowAnyException();
    }

    @Test
    void addSession_eventNotFound_throwsResourceNotFound() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.addSession(99L, makeReq(BASE, BASE.plusHours(1))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void addSession_eventNotFound_neverSaves() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.addSession(99L, makeReq(BASE, BASE.plusHours(1))));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void addSession_savedWithCorrectFields() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        LocalDateTime start = BASE;
        LocalDateTime end   = BASE.plusHours(2);
        Schedule saved = Schedule.builder().id(8L).event(event).sessionTitle("Keynote")
                .description("Opening keynote").startTime(start).endTime(end).build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(8L).build());

        scheduleService.addSession(1L, makeReq(start, end));

        ArgumentCaptor<Schedule> cap = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(cap.capture());
        assertThat(cap.getValue().getSessionTitle()).isEqualTo("Keynote");
        assertThat(cap.getValue().getStartTime()).isEqualTo(start);
        assertThat(cap.getValue().getEndTime()).isEqualTo(end);
        assertThat(cap.getValue().getEvent().getId()).isEqualTo(1L);
    }

    @Test
    void addSession_withNullDescription_savesNullDescription() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        CreateScheduleRequest req = makeReq(BASE, BASE.plusHours(1));
        req.setDescription(null);
        Schedule saved = Schedule.builder().id(9L).event(event).sessionTitle("Keynote")
                .startTime(BASE).endTime(BASE.plusHours(1)).build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(9L).build());

        assertThatCode(() -> scheduleService.addSession(1L, req)).doesNotThrowAnyException();

        ArgumentCaptor<Schedule> cap = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(cap.capture());
        assertThat(cap.getValue().getDescription()).isNull();
    }
}