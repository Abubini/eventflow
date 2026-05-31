// src/test/java/com/ctbe/eventflow/repository/RegistrationRepositoryTest.java
package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RegistrationRepositoryTest {

    @Autowired RegistrationRepository registrationRepository;
    @Autowired UserRepository userRepository;
    @Autowired EventRepository eventRepository;

    private User attendee;
    private Event event;

    @BeforeEach
    void setUp() {
        User organizer = userRepository.save(User.builder().name("Org").email("org@test.com")
                .passwordHash("hash").role(UserRole.ORGANIZER).active(true).build());
        attendee = userRepository.save(User.builder().name("Att").email("att@test.com")
                .passwordHash("hash").role(UserRole.ATTENDEE).active(true).build());
        event = eventRepository.save(Event.builder().title("Test Event").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.PUBLISHED)
                .capacity(50).createdBy(organizer).build());
    }

    @Test
    void findByUserAndEvent_existingRegistration_returnsIt() {
        Registration reg = registrationRepository.save(
                Registration.builder().user(attendee).event(event).status(RegStatus.CONFIRMED).build());

        Optional<Registration> found = registrationRepository.findByUserAndEvent(attendee, event);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(reg.getId());
    }

    @Test
    void findByUserAndEvent_noRegistration_returnsEmpty() {
        Optional<Registration> found = registrationRepository.findByUserAndEvent(attendee, event);
        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserIdAndEventId_registered_returnsTrue() {
        registrationRepository.save(
                Registration.builder().user(attendee).event(event).status(RegStatus.CONFIRMED).build());

        assertThat(registrationRepository.existsByUserIdAndEventId(attendee.getId(), event.getId())).isTrue();
    }

    @Test
    void existsByUserIdAndEventId_notRegistered_returnsFalse() {
        assertThat(registrationRepository.existsByUserIdAndEventId(attendee.getId(), event.getId())).isFalse();
    }

    @Test
    void countByEventAndStatus_confirmed_returnsCount() {
        registrationRepository.save(
                Registration.builder().user(attendee).event(event).status(RegStatus.CONFIRMED).build());

        long count = registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED);

        assertThat(count).isEqualTo(1L);
    }

    @Test
    void countByEventAndStatus_wrongStatus_returnsZero() {
        registrationRepository.save(
                Registration.builder().user(attendee).event(event).status(RegStatus.CONFIRMED).build());

        long count = registrationRepository.countByEventAndStatus(event, RegStatus.CANCELLED);

        assertThat(count).isEqualTo(0L);
    }

    @Test
    void findByEvent_returnsAllRegistrationsForEvent() {
        registrationRepository.save(
                Registration.builder().user(attendee).event(event).status(RegStatus.CONFIRMED).build());

        Page<Registration> page = registrationRepository.findByEvent(event, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByUser_returnsAllUserRegistrations() {
        registrationRepository.save(
                Registration.builder().user(attendee).event(event).status(RegStatus.CONFIRMED).build());

        Page<Registration> page = registrationRepository.findByUser(attendee, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser().getId()).isEqualTo(attendee.getId());
    }
}