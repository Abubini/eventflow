// src/test/java/com/ctbe/eventflow/repository/EventRepositoryTest.java
package com.ctbe.eventflow.repository;

import com.ctbe.eventflow.model.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired EventRepository eventRepository;
    @Autowired UserRepository userRepository;

    private User organizer;

    @BeforeEach
    void setUp() {
        organizer = userRepository.save(
                User.builder().name("Organizer").email("org@test.com")
                        .passwordHash("hash").role(UserRole.ORGANIZER).active(true).build());
    }

    // ── findByStatus ──────────────────────────────────────────

    @Test
    void findByStatus_publishedOnly_returnsPublished() {
        eventRepository.save(Event.builder().title("Draft").location("A")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.DRAFT).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("Published").location("B")
                .dateTime(LocalDateTime.now().plusDays(2)).status(EventStatus.PUBLISHED).createdBy(organizer).build());

        Page<Event> page = eventRepository.findByStatus(EventStatus.PUBLISHED, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Published");
    }

    @Test
    void findByStatus_cancelled_returnsCancelled() {
        eventRepository.save(Event.builder().title("Cancelled Event").location("C")
                .dateTime(LocalDateTime.now().plusDays(3)).status(EventStatus.CANCELLED).createdBy(organizer).build());

        Page<Event> page = eventRepository.findByStatus(EventStatus.CANCELLED, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByStatus_emptyDb_returnsEmptyPage() {
        Page<Event> page = eventRepository.findByStatus(EventStatus.PUBLISHED, PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    // ── findByCreatedBy ───────────────────────────────────────

    @Test
    void findByCreatedBy_returnsOnlyOrganizerEvents() {
        User other = userRepository.save(User.builder().name("Other").email("other@test.com")
                .passwordHash("hash").role(UserRole.ORGANIZER).active(true).build());

        eventRepository.save(Event.builder().title("My Event").location("A")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.DRAFT).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("Their Event").location("B")
                .dateTime(LocalDateTime.now().plusDays(2)).status(EventStatus.DRAFT).createdBy(other).build());

        List<Event> results = eventRepository.findByCreatedBy(organizer);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("My Event");
    }

    // ── search ────────────────────────────────────────────────

    @Test
    void search_byKeyword_matchesTitle() {
        eventRepository.save(Event.builder().title("Tech Conference").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(3)).status(EventStatus.PUBLISHED).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("Music Festival").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(4)).status(EventStatus.PUBLISHED).createdBy(organizer).build());

        List<Event> results = eventRepository.search("Tech", null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Tech Conference");
    }

    @Test
    void search_byKeyword_caseInsensitive() {
        eventRepository.save(Event.builder().title("Tech Conference").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(3)).status(EventStatus.PUBLISHED).createdBy(organizer).build());

        List<Event> results = eventRepository.search("tech", null, null, null, null);

        assertThat(results).hasSize(1);
    }

    @Test
    void search_byLocation_returnsMatching() {
        eventRepository.save(Event.builder().title("Event A").location("Addis Ababa")
                .dateTime(LocalDateTime.now().plusDays(3)).status(EventStatus.PUBLISHED).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("Event B").location("Nairobi")
                .dateTime(LocalDateTime.now().plusDays(4)).status(EventStatus.PUBLISHED).createdBy(organizer).build());

        List<Event> results = eventRepository.search(null, "Addis", null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Event A");
    }

    @Test
    void search_byStatus_returnsMatchingStatus() {
        eventRepository.save(Event.builder().title("Draft Event").location("X")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.DRAFT).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("Published Event").location("Y")
                .dateTime(LocalDateTime.now().plusDays(2)).status(EventStatus.PUBLISHED).createdBy(organizer).build());

        List<Event> results = eventRepository.search(null, null, EventStatus.PUBLISHED, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Published Event");
    }

    @Test
    void search_byDateRange_returnsInRange() {
        LocalDateTime base = LocalDateTime.now().plusDays(10);
        eventRepository.save(Event.builder().title("In Range").location("A")
                .dateTime(base).status(EventStatus.PUBLISHED).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("Out of Range").location("B")
                .dateTime(base.plusDays(20)).status(EventStatus.PUBLISHED).createdBy(organizer).build());

        List<Event> results = eventRepository.search(null, null, null,
                base.minusDays(1), base.plusDays(1));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("In Range");
    }

    @Test
    void search_allNullParams_returnsAllEvents() {
        eventRepository.save(Event.builder().title("E1").location("A")
                .dateTime(LocalDateTime.now().plusDays(1)).status(EventStatus.PUBLISHED).createdBy(organizer).build());
        eventRepository.save(Event.builder().title("E2").location("B")
                .dateTime(LocalDateTime.now().plusDays(2)).status(EventStatus.DRAFT).createdBy(organizer).build());

        List<Event> results = eventRepository.search(null, null, null, null, null);

        assertThat(results).hasSize(2);
    }
}