// src/test/java/com/ctbe/eventflow/service/ScheduleServiceTest.java
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
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock ScheduleMapper scheduleMapper;

    @InjectMocks ScheduleService scheduleService;

    private User organizer;
    private User otherUser;
    private User staffUser;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).email("org@example.com").role(UserRole.ORGANIZER).build();
        otherUser = User.builder().id(2L).email("other@example.com").role(UserRole.ORGANIZER).build();
        staffUser = User.builder().id(3L).email("staff@example.com").role(UserRole.STAFF).build();
        event = Event.builder().id(1L).title("Event").status(EventStatus.PUBLISHED).createdBy(organizer).build();
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

    private CreateScheduleRequest validRequest() {
        CreateScheduleRequest req = new CreateScheduleRequest();
        req.setSessionTitle("Keynote");
        req.setDescription("Opening talk");
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        return req;
    }

    // ── getSchedules ──────────────────────────────────────────

    @Test
    void getSchedules_returnsListForEvent() {
        Schedule schedule = Schedule.builder().id(1L).event(event).sessionTitle("Keynote").build();
        ScheduleDTO dto = ScheduleDTO.builder().id(1L).sessionTitle("Keynote").build();
        when(scheduleRepository.findByEventId(1L)).thenReturn(List.of(schedule));
        when(scheduleMapper.toDTO(schedule)).thenReturn(dto);

        List<ScheduleDTO> result = scheduleService.getSchedules(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionTitle()).isEqualTo("Keynote");
    }

    @Test
    void getSchedules_noSchedules_returnsEmptyList() {
        when(scheduleRepository.findByEventId(99L)).thenReturn(List.of());

        List<ScheduleDTO> result = scheduleService.getSchedules(99L);

        assertThat(result).isEmpty();
    }

    // ── addSession ────────────────────────────────────────────

    @Test
    void addSession_byOrganizer_savesSession() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        Schedule saved = Schedule.builder().id(5L).event(event).sessionTitle("Keynote").build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(5L).sessionTitle("Keynote").build());

        ScheduleDTO result = scheduleService.addSession(1L, validRequest());

        assertThat(result.getId()).isEqualTo(5L);
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void addSession_byStaff_succeeds() {
        mockSecurityAs(staffUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        Schedule saved = Schedule.builder().id(6L).event(event).sessionTitle("Keynote").build();
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(scheduleMapper.toDTO(saved)).thenReturn(ScheduleDTO.builder().id(6L).build());

        assertThatCode(() -> scheduleService.addSession(1L, validRequest())).doesNotThrowAnyException();
    }

    @Test
    void addSession_byOtherUser_throwsForbidden() {
        mockSecurityAs(otherUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> scheduleService.addSession(1L, validRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("organizer");
    }

    @Test
    void addSession_endTimeBeforeStartTime_throwsBadRequest() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        CreateScheduleRequest req = new CreateScheduleRequest();
        req.setSessionTitle("Bad Session");
        req.setStartTime(LocalDateTime.now().plusDays(2));
        req.setEndTime(LocalDateTime.now().plusDays(1)); // end before start

        assertThatThrownBy(() -> scheduleService.addSession(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void addSession_endTimeEqualsStartTime_throwsBadRequest() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        LocalDateTime time = LocalDateTime.now().plusDays(1);
        CreateScheduleRequest req = new CreateScheduleRequest();
        req.setSessionTitle("Zero Duration");
        req.setStartTime(time);
        req.setEndTime(time); // equal

        assertThatThrownBy(() -> scheduleService.addSession(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void addSession_eventNotFound_throwsResourceNotFound() {
        mockSecurityAs(organizer);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.addSession(99L, validRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}