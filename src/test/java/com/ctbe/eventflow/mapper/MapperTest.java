package com.ctbe.eventflow.mapper;

import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.RegistrationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapperTest {

    @Mock RegistrationRepository registrationRepository;

    private UserMapper         userMapper;
    private ScheduleMapper     scheduleMapper;
    private RegistrationMapper registrationMapper;
    private EventMapper        eventMapper;

    private User    user;
    private Event   event;
    private final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 10, 0);

    @BeforeEach
    void setUp() {
        userMapper         = new UserMapper();
        scheduleMapper     = new ScheduleMapper();
        registrationMapper = new RegistrationMapper();
        eventMapper        = new EventMapper(userMapper, registrationRepository);

        user = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .role(UserRole.ATTENDEE).active(true).createdAt(NOW)
                .passwordHash("hashed").build();

        event = Event.builder()
                .id(10L).title("Tech Conf").description("Annual conference")
                .location("Addis Ababa").dateTime(NOW.plusDays(10))
                .capacity(200).status(EventStatus.PUBLISHED)
                .createdBy(user).createdAt(NOW).build();
    }

    // ════════════════════════════════════════════════════
    //  UserMapper
    // ════════════════════════════════════════════════════

    @Test
    void userMapper_mapsId() {
        assertThat(userMapper.toDTO(user).getId()).isEqualTo(1L);
    }

    @Test
    void userMapper_mapsName() {
        assertThat(userMapper.toDTO(user).getName()).isEqualTo("Alice");
    }

    @Test
    void userMapper_mapsEmail() {
        assertThat(userMapper.toDTO(user).getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void userMapper_mapsRole() {
        assertThat(userMapper.toDTO(user).getRole()).isEqualTo(UserRole.ATTENDEE);
    }

    @Test
    void userMapper_mapsActiveTrue() {
        assertThat(userMapper.toDTO(user).isActive()).isTrue();
    }

    @Test
    void userMapper_mapsActiveFalse() {
        user.setActive(false);
        assertThat(userMapper.toDTO(user).isActive()).isFalse();
    }

    @Test
    void userMapper_mapsCreatedAt() {
        assertThat(userMapper.toDTO(user).getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void userMapper_organizerRole_mappedCorrectly() {
        user.setRole(UserRole.ORGANIZER);
        assertThat(userMapper.toDTO(user).getRole()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    void userMapper_staffRole_mappedCorrectly() {
        user.setRole(UserRole.STAFF);
        assertThat(userMapper.toDTO(user).getRole()).isEqualTo(UserRole.STAFF);
    }

    @Test
    void userMapper_nullCreatedAt_mapsAsNull() {
        user.setCreatedAt(null);
        assertThat(userMapper.toDTO(user).getCreatedAt()).isNull();
    }

    @Test
    void userMapper_passwordHashNotExposedInDTO() {
        UserDTO dto = userMapper.toDTO(user);
        // UserDTO has no password field — just verify it compiles and runs
        assertThat(dto).isNotNull();
        // The DTO class has no getPasswordHash — this test ensures we never add it accidentally
        assertThat(dto.getClass().getDeclaredFields())
                .noneMatch(f -> f.getName().toLowerCase().contains("password"));
    }

    // ════════════════════════════════════════════════════
    //  EventMapper
    // ════════════════════════════════════════════════════

    @Test
    void eventMapper_mapsId() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getId()).isEqualTo(10L);
    }

    @Test
    void eventMapper_mapsTitle() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getTitle()).isEqualTo("Tech Conf");
    }

    @Test
    void eventMapper_mapsDescription() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getDescription()).isEqualTo("Annual conference");
    }

    @Test
    void eventMapper_mapsLocation() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getLocation()).isEqualTo("Addis Ababa");
    }

    @Test
    void eventMapper_mapsDateTime() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getDateTime()).isEqualTo(NOW.plusDays(10));
    }

    @Test
    void eventMapper_mapsCapacity() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getCapacity()).isEqualTo(200);
    }

    @Test
    void eventMapper_mapsStatus() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void eventMapper_mapsCreatedAt() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void eventMapper_registeredCount_zeroWhenNoRegistrations() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getRegisteredCount()).isZero();
    }

    @Test
    void eventMapper_registeredCount_reflectsRepositoryValue() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(42L);
        assertThat(eventMapper.toDTO(event).getRegisteredCount()).isEqualTo(42L);
    }

    @Test
    void eventMapper_registeredCount_queriesOnlyConfirmed() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(10L);
        eventMapper.toDTO(event);
        verify(registrationRepository).countByEventAndStatus(event, RegStatus.CONFIRMED);
        verify(registrationRepository, never()).countByEventAndStatus(event, RegStatus.CANCELLED);
    }

    @Test
    void eventMapper_createdBy_mapsNestedUser() {
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        EventDTO dto = eventMapper.toDTO(event);
        assertThat(dto.getCreatedBy()).isNotNull();
        assertThat(dto.getCreatedBy().getId()).isEqualTo(user.getId());
        assertThat(dto.getCreatedBy().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void eventMapper_nullCapacity_mapsAsNull() {
        event.setCapacity(null);
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getCapacity()).isNull();
    }

    @Test
    void eventMapper_nullDescription_mapsAsNull() {
        event.setDescription(null);
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getDescription()).isNull();
    }

    @Test
    void eventMapper_draftStatus_mappedCorrectly() {
        event.setStatus(EventStatus.DRAFT);
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void eventMapper_cancelledStatus_mappedCorrectly() {
        event.setStatus(EventStatus.CANCELLED);
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(0L);
        assertThat(eventMapper.toDTO(event).getStatus()).isEqualTo(EventStatus.CANCELLED);
    }

    // ════════════════════════════════════════════════════
    //  RegistrationMapper
    // ════════════════════════════════════════════════════

    @Test
    void registrationMapper_mapsId() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        assertThat(registrationMapper.toDTO(reg).getId()).isEqualTo(1L);
    }

    @Test
    void registrationMapper_mapsEventId() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        assertThat(registrationMapper.toDTO(reg).getEventId()).isEqualTo(event.getId());
    }

    @Test
    void registrationMapper_mapsEventTitle() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        assertThat(registrationMapper.toDTO(reg).getEventTitle()).isEqualTo("Tech Conf");
    }

    @Test
    void registrationMapper_mapsUserId() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        assertThat(registrationMapper.toDTO(reg).getUserId()).isEqualTo(user.getId());
    }

    @Test
    void registrationMapper_mapsUserName() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        assertThat(registrationMapper.toDTO(reg).getUserName()).isEqualTo("Alice");
    }

    @Test
    void registrationMapper_mapsStatus_confirmed() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        assertThat(registrationMapper.toDTO(reg).getStatus()).isEqualTo(RegStatus.CONFIRMED);
    }

    @Test
    void registrationMapper_mapsStatus_cancelled() {
        Registration reg = buildReg(1L, RegStatus.CANCELLED);
        assertThat(registrationMapper.toDTO(reg).getStatus()).isEqualTo(RegStatus.CANCELLED);
    }

    @Test
    void registrationMapper_mapsRegisteredAt() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        reg.setRegisteredAt(NOW);
        assertThat(registrationMapper.toDTO(reg).getRegisteredAt()).isEqualTo(NOW);
    }

    @Test
    void registrationMapper_nullRegisteredAt_mapsAsNull() {
        Registration reg = buildReg(1L, RegStatus.CONFIRMED);
        reg.setRegisteredAt(null);
        assertThat(registrationMapper.toDTO(reg).getRegisteredAt()).isNull();
    }

    // ════════════════════════════════════════════════════
    //  ScheduleMapper
    // ════════════════════════════════════════════════════

    @Test
    void scheduleMapper_mapsId() {
        assertThat(scheduleMapper.toDTO(buildSchedule()).getId()).isEqualTo(5L);
    }

    @Test
    void scheduleMapper_mapsEventId() {
        assertThat(scheduleMapper.toDTO(buildSchedule()).getEventId()).isEqualTo(event.getId());
    }

    @Test
    void scheduleMapper_mapsSessionTitle() {
        assertThat(scheduleMapper.toDTO(buildSchedule()).getSessionTitle()).isEqualTo("Keynote");
    }

    @Test
    void scheduleMapper_mapsDescription() {
        assertThat(scheduleMapper.toDTO(buildSchedule()).getDescription()).isEqualTo("Opening talk");
    }

    @Test
    void scheduleMapper_mapsStartTime() {
        assertThat(scheduleMapper.toDTO(buildSchedule()).getStartTime()).isEqualTo(NOW);
    }

    @Test
    void scheduleMapper_mapsEndTime() {
        assertThat(scheduleMapper.toDTO(buildSchedule()).getEndTime()).isEqualTo(NOW.plusHours(2));
    }

    @Test
    void scheduleMapper_mapsCreatedAt() {
        Schedule s = buildSchedule();
        s.setCreatedAt(NOW);
        assertThat(scheduleMapper.toDTO(s).getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void scheduleMapper_nullDescription_mapsAsNull() {
        Schedule s = buildSchedule();
        s.setDescription(null);
        assertThat(scheduleMapper.toDTO(s).getDescription()).isNull();
    }

    @Test
    void scheduleMapper_nullCreatedAt_mapsAsNull() {
        Schedule s = buildSchedule();
        s.setCreatedAt(null);
        assertThat(scheduleMapper.toDTO(s).getCreatedAt()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────

    private Registration buildReg(Long id, RegStatus status) {
        return Registration.builder()
                .id(id).user(user).event(event).status(status)
                .registeredAt(NOW).build();
    }

    private Schedule buildSchedule() {
        return Schedule.builder()
                .id(5L).event(event).sessionTitle("Keynote")
                .description("Opening talk").startTime(NOW)
                .endTime(NOW.plusHours(2)).createdAt(NOW).build();
    }
}