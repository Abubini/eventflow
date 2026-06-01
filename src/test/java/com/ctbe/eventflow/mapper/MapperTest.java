// src/test/java/com/ctbe/eventflow/mapper/MapperTest.java
package com.ctbe.eventflow.mapper;

import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapperTest {

    @Mock RegistrationRepository registrationRepository;

    private UserMapper userMapper;
    private ScheduleMapper scheduleMapper;
    private RegistrationMapper registrationMapper;
    private EventMapper eventMapper;

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        scheduleMapper = new ScheduleMapper();
        registrationMapper = new RegistrationMapper();
        eventMapper = new EventMapper(userMapper, registrationRepository);

        user = User.builder().id(1L).name("Alice").email("alice@example.com")
                .role(UserRole.ATTENDEE).active(true)
                .createdAt(LocalDateTime.now()).build();

        event = Event.builder().id(1L).title("Tech Conf").description("Great event")
                .location("Addis Ababa").dateTime(LocalDateTime.now().plusDays(5))
                .capacity(200).status(EventStatus.PUBLISHED).createdBy(user)
                .createdAt(LocalDateTime.now()).build();
    }

    // ── UserMapper ────────────────────────────────────────────

    @Test
    void userMapper_mapsAllFields() {
        UserDTO dto = userMapper.toDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getRole()).isEqualTo(UserRole.ATTENDEE);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getCreatedAt()).isNotNull();
    }

    // ── ScheduleMapper ────────────────────────────────────────

    @Test
    void scheduleMapper_mapsAllFields() {
        Schedule schedule = Schedule.builder().id(5L).event(event)
                .sessionTitle("Keynote").description("Opening")
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .createdAt(LocalDateTime.now()).build();

        ScheduleDTO dto = scheduleMapper.toDTO(schedule);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getEventId()).isEqualTo(1L);
        assertThat(dto.getSessionTitle()).isEqualTo("Keynote");
        assertThat(dto.getDescription()).isEqualTo("Opening");
    }

    // ── RegistrationMapper ────────────────────────────────────

    @Test
    void registrationMapper_mapsAllFields() {
        Registration reg = Registration.builder().id(10L).user(user).event(event)
                .status(RegStatus.CONFIRMED).registeredAt(LocalDateTime.now()).build();

        RegistrationDTO dto = registrationMapper.toDTO(reg);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getUserName()).isEqualTo("Alice");
        assertThat(dto.getEventId()).isEqualTo(1L);
        assertThat(dto.getEventTitle()).isEqualTo("Tech Conf");
        assertThat(dto.getStatus()).isEqualTo(RegStatus.CONFIRMED);
    }

    // ── EventMapper ───────────────────────────────────────────

    @Test
    void eventMapper_mapsAllFields() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(42L);

        EventDTO dto = eventMapper.toDTO(event);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Tech Conf");
        assertThat(dto.getLocation()).isEqualTo("Addis Ababa");
        assertThat(dto.getCapacity()).isEqualTo(200);
        assertThat(dto.getRegisteredCount()).isEqualTo(42L);
        assertThat(dto.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(dto.getCreatedBy().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void eventMapper_registeredCountComesFromRepository() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(15L);

        EventDTO dto = eventMapper.toDTO(event);

        assertThat(dto.getRegisteredCount()).isEqualTo(15L);
    }
}