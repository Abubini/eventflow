package com.ctbe.eventflow.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.*;

class ConverterTest {

    private final UserRoleConverter    userRoleConverter    = new UserRoleConverter();
    private final EventStatusConverter eventStatusConverter = new EventStatusConverter();
    private final RegStatusConverter   regStatusConverter   = new RegStatusConverter();

    // ── UserRoleConverter ─────────────────────────────────────

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void userRole_toDatabaseColumn_returnsName(UserRole role) {
        assertThat(userRoleConverter.convertToDatabaseColumn(role)).isEqualTo(role.name());
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void userRole_toEntityAttribute_returnsEnum(UserRole role) {
        assertThat(userRoleConverter.convertToEntityAttribute(role.name())).isEqualTo(role);
    }

    @Test
    void userRole_toDatabaseColumn_null_returnsNull() {
        assertThat(userRoleConverter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void userRole_toEntityAttribute_null_returnsNull() {
        assertThat(userRoleConverter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void userRole_roundTrip_attendee() {
        UserRole original = UserRole.ATTENDEE;
        String db = userRoleConverter.convertToDatabaseColumn(original);
        assertThat(userRoleConverter.convertToEntityAttribute(db)).isEqualTo(original);
    }

    @Test
    void userRole_roundTrip_organizer() {
        UserRole original = UserRole.ORGANIZER;
        String db = userRoleConverter.convertToDatabaseColumn(original);
        assertThat(userRoleConverter.convertToEntityAttribute(db)).isEqualTo(original);
    }

    @Test
    void userRole_roundTrip_staff() {
        UserRole original = UserRole.STAFF;
        String db = userRoleConverter.convertToDatabaseColumn(original);
        assertThat(userRoleConverter.convertToEntityAttribute(db)).isEqualTo(original);
    }

    // ── EventStatusConverter ──────────────────────────────────

    @ParameterizedTest
    @EnumSource(EventStatus.class)
    void eventStatus_toDatabaseColumn_returnsName(EventStatus status) {
        assertThat(eventStatusConverter.convertToDatabaseColumn(status)).isEqualTo(status.name());
    }

    @ParameterizedTest
    @EnumSource(EventStatus.class)
    void eventStatus_toEntityAttribute_returnsEnum(EventStatus status) {
        assertThat(eventStatusConverter.convertToEntityAttribute(status.name())).isEqualTo(status);
    }

    @Test
    void eventStatus_toDatabaseColumn_null_returnsNull() {
        assertThat(eventStatusConverter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void eventStatus_toEntityAttribute_null_returnsNull() {
        assertThat(eventStatusConverter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void eventStatus_roundTrip_draft() {
        String db = eventStatusConverter.convertToDatabaseColumn(EventStatus.DRAFT);
        assertThat(eventStatusConverter.convertToEntityAttribute(db)).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void eventStatus_roundTrip_published() {
        String db = eventStatusConverter.convertToDatabaseColumn(EventStatus.PUBLISHED);
        assertThat(eventStatusConverter.convertToEntityAttribute(db)).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void eventStatus_roundTrip_cancelled() {
        String db = eventStatusConverter.convertToDatabaseColumn(EventStatus.CANCELLED);
        assertThat(eventStatusConverter.convertToEntityAttribute(db)).isEqualTo(EventStatus.CANCELLED);
    }

    // ── RegStatusConverter ────────────────────────────────────

    @ParameterizedTest
    @EnumSource(RegStatus.class)
    void regStatus_toDatabaseColumn_returnsName(RegStatus status) {
        assertThat(regStatusConverter.convertToDatabaseColumn(status)).isEqualTo(status.name());
    }

    @ParameterizedTest
    @EnumSource(RegStatus.class)
    void regStatus_toEntityAttribute_returnsEnum(RegStatus status) {
        assertThat(regStatusConverter.convertToEntityAttribute(status.name())).isEqualTo(status);
    }

    @Test
    void regStatus_toDatabaseColumn_null_returnsNull() {
        assertThat(regStatusConverter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void regStatus_toEntityAttribute_null_returnsNull() {
        assertThat(regStatusConverter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void regStatus_roundTrip_confirmed() {
        String db = regStatusConverter.convertToDatabaseColumn(RegStatus.CONFIRMED);
        assertThat(regStatusConverter.convertToEntityAttribute(db)).isEqualTo(RegStatus.CONFIRMED);
    }

    @Test
    void regStatus_roundTrip_cancelled() {
        String db = regStatusConverter.convertToDatabaseColumn(RegStatus.CANCELLED);
        assertThat(regStatusConverter.convertToEntityAttribute(db)).isEqualTo(RegStatus.CANCELLED);
    }

    @Test
    void regStatus_roundTrip_waitlisted() {
        String db = regStatusConverter.convertToDatabaseColumn(RegStatus.WAITLISTED);
        assertThat(regStatusConverter.convertToEntityAttribute(db)).isEqualTo(RegStatus.WAITLISTED);
    }
}