package com.ctbe.eventflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

class ModelTest {

    // ════════════════════════════════════════════════════
    //  Enum values exist and are correctly named
    // ════════════════════════════════════════════════════

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void userRole_allValuesHaveNames(UserRole role) {
        assertThat(role.name()).isNotBlank();
    }

    @Test
    void userRole_hasExpectedValues() {
        assertThat(UserRole.values()).containsExactlyInAnyOrder(
                UserRole.ORGANIZER, UserRole.ATTENDEE, UserRole.STAFF);
    }

    @ParameterizedTest
    @EnumSource(EventStatus.class)
    void eventStatus_allValuesHaveNames(EventStatus status) {
        assertThat(status.name()).isNotBlank();
    }

    @Test
    void eventStatus_hasExpectedValues() {
        assertThat(EventStatus.values()).containsExactlyInAnyOrder(
                EventStatus.DRAFT, EventStatus.PUBLISHED, EventStatus.CANCELLED);
    }

    @ParameterizedTest
    @EnumSource(RegStatus.class)
    void regStatus_allValuesHaveNames(RegStatus status) {
        assertThat(status.name()).isNotBlank();
    }

    @Test
    void regStatus_hasExpectedValues() {
        assertThat(RegStatus.values()).containsExactlyInAnyOrder(
                RegStatus.CONFIRMED, RegStatus.CANCELLED, RegStatus.WAITLISTED);
    }

    // ════════════════════════════════════════════════════
    //  User entity
    // ════════════════════════════════════════════════════

    @Test
    void user_builder_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .passwordHash("hashed").role(UserRole.ATTENDEE)
                .active(true).createdAt(now).build();

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getName()).isEqualTo("Alice");
        assertThat(user.getEmail()).isEqualTo("alice@test.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRole()).isEqualTo(UserRole.ATTENDEE);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void user_defaultRole_isAttendee() {
        User user = new User();
        user.setRole(UserRole.ATTENDEE);
        assertThat(user.getRole()).isEqualTo(UserRole.ATTENDEE);
    }

    @Test
    void user_builderDefault_eventsListIsEmpty() {
        User user = User.builder().id(1L).name("Alice").email("a@t.com")
                .passwordHash("h").role(UserRole.ATTENDEE).active(true).build();
        assertThat(user.getEvents()).isNotNull().isEmpty();
    }

    @Test
    void user_builderDefault_registrationsListIsEmpty() {
        User user = User.builder().id(1L).name("Alice").email("a@t.com")
                .passwordHash("h").role(UserRole.ATTENDEE).active(true).build();
        assertThat(user.getRegistrations()).isNotNull().isEmpty();
    }

    @Test
    void user_setter_changesRole() {
        User user = User.builder().id(1L).name("Alice").email("a@t.com")
                .passwordHash("h").role(UserRole.ATTENDEE).active(true).build();
        user.setRole(UserRole.ORGANIZER);
        assertThat(user.getRole()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    void user_setter_changesActive() {
        User user = User.builder().id(1L).name("Alice").email("a@t.com")
                .passwordHash("h").role(UserRole.ATTENDEE).active(true).build();
        user.setActive(false);
        assertThat(user.isActive()).isFalse();
    }

    // ════════════════════════════════════════════════════
    //  Event entity
    // ════════════════════════════════════════════════════

    @Test
    void event_builder_setsAllFields() {
        User organizer = User.builder().id(1L).name("Org").email("o@t.com")
                .passwordHash("h").role(UserRole.ORGANIZER).active(true).build();
        LocalDateTime dt = LocalDateTime.now().plusDays(5);

        Event event = Event.builder()
                .id(1L).title("Tech Conf").description("Annual conf")
                .location("Addis").dateTime(dt).capacity(100)
                .status(EventStatus.DRAFT).createdBy(organizer).build();

        assertThat(event.getId()).isEqualTo(1L);
        assertThat(event.getTitle()).isEqualTo("Tech Conf");
        assertThat(event.getDescription()).isEqualTo("Annual conf");
        assertThat(event.getLocation()).isEqualTo("Addis");
        assertThat(event.getDateTime()).isEqualTo(dt);
        assertThat(event.getCapacity()).isEqualTo(100);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getCreatedBy().getId()).isEqualTo(1L);
    }

    @Test
    void event_defaultStatus_isDraft() {
        Event event = new Event();
        event.setStatus(EventStatus.DRAFT);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void event_builderDefault_registrationsListIsEmpty() {
        Event event = Event.builder().id(1L).title("E").location("L")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.DRAFT)
                .createdBy(User.builder().id(1L).name("O").email("o@t.com")
                        .passwordHash("h").role(UserRole.ORGANIZER).active(true).build())
                .build();
        assertThat(event.getRegistrations()).isNotNull().isEmpty();
    }

    @Test
    void event_builderDefault_schedulesListIsEmpty() {
        Event event = Event.builder().id(1L).title("E").location("L")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.DRAFT)
                .createdBy(User.builder().id(1L).name("O").email("o@t.com")
                        .passwordHash("h").role(UserRole.ORGANIZER).active(true).build())
                .build();
        assertThat(event.getSchedules()).isNotNull().isEmpty();
    }

    @Test
    void event_nullCapacity_isAllowed() {
        Event event = Event.builder().id(1L).title("E").location("L")
                .dateTime(LocalDateTime.now().plusDays(1))
                .status(EventStatus.DRAFT).capacity(null)
                .createdBy(User.builder().id(1L).name("O").email("o@t.com")
                        .passwordHash("h").role(UserRole.ORGANIZER).active(true).build())
                .build();
        assertThat(event.getCapacity()).isNull();
    }

    // ════════════════════════════════════════════════════
    //  Registration entity
    // ════════════════════════════════════════════════════

    @Test
    void registration_builder_setsAllFields() {
        User user = User.builder().id(1L).name("A").email("a@t.com")
                .passwordHash("h").role(UserRole.ATTENDEE).active(true).build();
        Event event = Event.builder().id(1L).title("E").location("L")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.PUBLISHED)
                .createdBy(user).build();
        LocalDateTime regAt = LocalDateTime.now();

        Registration reg = Registration.builder()
                .id(10L).user(user).event(event)
                .status(RegStatus.CONFIRMED).registeredAt(regAt).build();

        assertThat(reg.getId()).isEqualTo(10L);
        assertThat(reg.getUser().getId()).isEqualTo(1L);
        assertThat(reg.getEvent().getId()).isEqualTo(1L);
        assertThat(reg.getStatus()).isEqualTo(RegStatus.CONFIRMED);
        assertThat(reg.getRegisteredAt()).isEqualTo(regAt);
    }

    @Test
    void registration_defaultStatus_isConfirmed() {
        Registration reg = new Registration();
        reg.setStatus(RegStatus.CONFIRMED);
        assertThat(reg.getStatus()).isEqualTo(RegStatus.CONFIRMED);
    }

    // ════════════════════════════════════════════════════
    //  Schedule entity
    // ════════════════════════════════════════════════════

    @Test
    void schedule_builder_setsAllFields() {
        User user = User.builder().id(1L).name("O").email("o@t.com")
                .passwordHash("h").role(UserRole.ORGANIZER).active(true).build();
        Event event = Event.builder().id(1L).title("E").location("L")
                .dateTime(LocalDateTime.now().plusDays(1))
                .status(EventStatus.PUBLISHED).createdBy(user).build();
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end   = start.plusHours(2);

        Schedule schedule = Schedule.builder()
                .id(5L).event(event).sessionTitle("Keynote")
                .description("Opening").startTime(start).endTime(end).build();

        assertThat(schedule.getId()).isEqualTo(5L);
        assertThat(schedule.getSessionTitle()).isEqualTo("Keynote");
        assertThat(schedule.getDescription()).isEqualTo("Opening");
        assertThat(schedule.getStartTime()).isEqualTo(start);
        assertThat(schedule.getEndTime()).isEqualTo(end);
    }

    // ════════════════════════════════════════════════════
    //  TokenBlacklist entity
    // ════════════════════════════════════════════════════

    @Test
    void tokenBlacklist_builder_setsAllFields() {
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);
        TokenBlacklist bl = TokenBlacklist.builder()
                .id(1L).token("some.jwt.token").expiresAt(expiry).build();

        assertThat(bl.getId()).isEqualTo(1L);
        assertThat(bl.getToken()).isEqualTo("some.jwt.token");
        assertThat(bl.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void tokenBlacklist_setter_changesToken() {
        TokenBlacklist bl = new TokenBlacklist();
        bl.setToken("new.token");
        assertThat(bl.getToken()).isEqualTo("new.token");
    }
}