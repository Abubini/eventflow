// src/test/java/com/ctbe/eventflow/service/RegistrationServiceTest.java
package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.BookingRequest;
import com.ctbe.eventflow.dto.request.ScanRequest;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.RegistrationMapper;
import com.ctbe.eventflow.mapper.UserMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock RegistrationRepository registrationRepository;
    @Mock EventRepository eventRepository;
    @Mock UserRepository userRepository;
    @Mock RegistrationMapper registrationMapper;
    @Mock UserMapper userMapper;
    @Mock TicketQrService ticketQrService;
    @Mock EmailService emailService;
    @Mock WaitlistRepository waitlistRepository;

    @InjectMocks RegistrationService registrationService;

    private User attendee;
    private User organizer;
    private User staffUser;
    private Event event;

    @BeforeEach
    void setUp() {
        attendee = User.builder().id(2L).email("att@example.com").role(UserRole.ATTENDEE).build();
        organizer = User.builder().id(1L).email("org@example.com").role(UserRole.ORGANIZER).build();
        staffUser = User.builder().id(3L).email("staff@example.com").role(UserRole.STAFF).build();

        event = Event.builder().id(1L).title("Test Event")
                .status(EventStatus.PUBLISHED).capacity(100).createdBy(organizer).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityAs(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ── register ──────────────────────────────────────────────

    @Test
    void register_happyPath_savesAndReturnsDTO() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(5L);
        Registration saved = Registration.builder().id(10L).user(attendee).event(event).status(RegStatus.CONFIRMED).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(RegistrationDTO.builder().id(10L).build());

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);
        RegistrationDTO result = registrationService.register(1L, req);

        assertThat(result.getId()).isEqualTo(10L);
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void register_eventNotFound_throwsResourceNotFound() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);

        assertThatThrownBy(() -> registrationService.register(99L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void register_eventNotPublished_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);

        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void register_cancelledEvent_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);

        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void register_alreadyRegistered_throwsConflict() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(true);

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);

        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Already registered");
    }

    @Test
    void register_atFullCapacity_throwsBadRequest() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(100L);

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);

        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("full capacity");
    }

    @Test
    void register_noCapacityLimit_alwaysAllows() {
        mockSecurityAs(attendee);
        event.setCapacity(null); // unlimited
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        Registration saved = Registration.builder().id(11L).user(attendee).event(event).status(RegStatus.CONFIRMED).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(RegistrationDTO.builder().id(11L).build());

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(1);

        assertThatCode(() -> registrationService.register(1L, req)).doesNotThrowAnyException();
    }

    // ── cancel ────────────────────────────────────────────────

    @Test
    void cancel_existingRegistration_deletesIt() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().plusDays(2));
        Registration reg = Registration.builder().id(10L).user(attendee).event(event).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(reg));
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of());

        registrationService.cancel(1L);

        verify(registrationRepository).delete(reg);
    }

    @Test
    void cancel_notRegistered_throwsResourceNotFound() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().plusDays(2));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancel(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Registration not found");
    }

    @Test
    void cancel_eventNotFound_throwsResourceNotFound() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancel(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAttendees ──────────────────────────────────────────

    @Test
    void getAttendees_byOrganizer_returnsPage() {
        mockSecurityAs(organizer);
        Page<Registration> page = new PageImpl<>(
                java.util.List.of(Registration.builder().user(attendee).event(event).build()));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEvent(eq(event), any(Pageable.class))).thenReturn(page);
        when(userMapper.toDTO(attendee)).thenReturn(UserDTO.builder().id(2L).build());

        Page<UserDTO> result = registrationService.getAttendees(1L, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getAttendees_byStaff_returnsPage() {
        mockSecurityAs(staffUser);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByEvent(eq(event), any(Pageable.class))).thenReturn(Page.empty());

        assertThatCode(() -> registrationService.getAttendees(1L, PageRequest.of(0, 20)))
                .doesNotThrowAnyException();
    }

    @Test
    void getAttendees_byAttendee_throwsForbidden() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.getAttendees(1L, PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getMyRegistrations_returnsPageForCurrentUser() {
        mockSecurityAs(attendee);
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(event).status(RegStatus.CONFIRMED)
                .ticketCode(UUID.randomUUID()).build();
        when(registrationRepository.findByUserOrderByRegisteredAtDesc(eq(attendee), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reg)));
        when(registrationMapper.toDTO(reg)).thenReturn(RegistrationDTO.builder().id(1L).build());

        Page<RegistrationDTO> result = registrationService.getMyRegistrations(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

// ── getTicket ─────────────────────────────────────────────

    @Test
    void getTicket_registeredUser_returnsTicketWithQr() {
        mockSecurityAs(attendee);
        UUID code = UUID.randomUUID();
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(event).status(RegStatus.CONFIRMED)
                .ticketCode(code).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(reg));
        when(ticketQrService.generateQrCodeBase64(code)).thenReturn("base64qr==");
        when(registrationMapper.toTicketDTO(reg, "base64qr=="))
                .thenReturn(TicketDTO.builder().ticketCode(code).qrCodeBase64("base64qr==").build());

        TicketDTO ticket = registrationService.getTicket(1L);

        assertThat(ticket.getTicketCode()).isEqualTo(code);
        assertThat(ticket.getQrCodeBase64()).isEqualTo("base64qr==");
    }

    @Test
    void getTicket_notRegistered_throwsResourceNotFound() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.getTicket(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

// ── scanTicket ────────────────────────────────────────────

    @Test
    void scanTicket_validTicket_marksScannedAndReturnsValid() {
        mockSecurityAs(organizer);
        UUID code = UUID.randomUUID();
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(event).status(RegStatus.CONFIRMED)
                .ticketCode(code).scanned(false).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByTicketCode(code)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(reg)).thenReturn(reg);

        ScanRequest req = new ScanRequest();
        req.setTicketCode(code);
        ScanResultDTO result = registrationService.scanTicket(1L, req);

        assertThat(result.isValid()).isTrue();
        assertThat(reg.isScanned()).isTrue();
        assertThat(reg.getScannedAt()).isNotNull();
    }

    @Test
    void scanTicket_alreadyScanned_returnsInvalid() {
        mockSecurityAs(organizer);
        UUID code = UUID.randomUUID();
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(event).status(RegStatus.CONFIRMED)
                .ticketCode(code).scanned(true).scannedAt(LocalDateTime.now().minusMinutes(5)).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByTicketCode(code)).thenReturn(Optional.of(reg));

        ScanRequest req = new ScanRequest();
        req.setTicketCode(code);
        ScanResultDTO result = registrationService.scanTicket(1L, req);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("already scanned");
    }

    @Test
    void scanTicket_unknownCode_returnsInvalid() {
        mockSecurityAs(organizer);
        UUID code = UUID.randomUUID();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByTicketCode(code)).thenReturn(Optional.empty());

        ScanRequest req = new ScanRequest();
        req.setTicketCode(code);
        ScanResultDTO result = registrationService.scanTicket(1L, req);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("not recognised");
    }

    @Test
    void scanTicket_wrongEvent_returnsInvalid() {
        mockSecurityAs(organizer);
        UUID code = UUID.randomUUID();
        Event otherEvent = Event.builder().id(99L).title("Other").createdBy(organizer).build();
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(otherEvent).status(RegStatus.CONFIRMED)
                .ticketCode(code).scanned(false).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByTicketCode(code)).thenReturn(Optional.of(reg));

        ScanRequest req = new ScanRequest();
        req.setTicketCode(code);
        ScanResultDTO result = registrationService.scanTicket(1L, req);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("different event");
    }

    @Test
    void scanTicket_byAttendee_throwsForbidden() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ScanRequest req = new ScanRequest();
        req.setTicketCode(UUID.randomUUID());

        assertThatThrownBy(() -> registrationService.scanTicket(1L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void scanTicket_cancelledRegistration_returnsInvalid() {
        mockSecurityAs(organizer);
        UUID code = UUID.randomUUID();
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(event).status(RegStatus.CANCELLED)
                .ticketCode(code).scanned(false).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByTicketCode(code)).thenReturn(Optional.of(reg));

        ScanRequest req = new ScanRequest();
        req.setTicketCode(code);
        ScanResultDTO result = registrationService.scanTicket(1L, req);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).contains("cancelled");
    }
    @Test
    void register_withMultipleAttendees_savesCorrectCount() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(90L);
        // capacity=100, used=90, requesting 5 → fine
        event.setCapacity(100);

        Registration saved = Registration.builder()
                .id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(5).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(
                RegistrationDTO.builder().id(1L).attendeeCount(5).build());

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(5);
        RegistrationDTO result = registrationService.register(1L, req);

        assertThat(result.getAttendeeCount()).isEqualTo(5);
    }

    @Test
    void register_requestedSeatsExceedAvailable_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setCapacity(100);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(98L);

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(5); // only 2 left

        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only 2 seat(s) left");
    }

// ── cancel date enforcement ───────────────────────────────

    @Test
    void cancel_afterEventStarted_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().minusHours(1)); // event already started
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.cancel(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("after the event has started");
    }

    @Test
    void cancel_beforeEventStarted_succeeds() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().plusDays(2));
        Registration reg = Registration.builder()
                .id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(2).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(reg));
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of());

        assertThatCode(() -> registrationService.cancel(1L)).doesNotThrowAnyException();
        verify(registrationRepository).delete(reg);
    }

// ── waitlist ──────────────────────────────────────────────

    @Test
    void joinWaitlist_eventFull_addsEntry() {
        mockSecurityAs(attendee);
        event.setCapacity(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(10L);
        WaitlistEntry entry = WaitlistEntry.builder()
                .id(1L).user(attendee).event(event).build();
        when(waitlistRepository.save(any())).thenReturn(entry);
        when(registrationMapper.toWaitlistDTO(entry)).thenReturn(
                WaitlistDTO.builder().id(1L).eventId(1L).build());

        WaitlistDTO result = registrationService.joinWaitlist(1L);

        assertThat(result.getEventId()).isEqualTo(1L);
    }

    @Test
    void joinWaitlist_slotsStillAvailable_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setCapacity(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(5L);

        assertThatThrownBy(() -> registrationService.joinWaitlist(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("seats available");
    }

    @Test
    void joinWaitlist_alreadyOnWaitlist_throwsConflict() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.joinWaitlist(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already on the waitlist");
    }

    @Test
    void notifyWaitlist_notifiesUpToReleasedSeatCount() {
        User waiter1 = User.builder().id(10L).email("w1@test.com").name("W1").build();
        User waiter2 = User.builder().id(11L).email("w2@test.com").name("W2").build();
        WaitlistEntry e1 = WaitlistEntry.builder().id(1L).user(waiter1).event(event).notified(false).build();
        WaitlistEntry e2 = WaitlistEntry.builder().id(2L).user(waiter2).event(event).notified(false).build();
        event.setCapacity(10);
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of(e1, e2));

        registrationService.notifyWaitlist(event, 1); // only 1 seat freed

        verify(emailService, times(1)).sendSlotAvailableNotification(any(), eq(event));
        assertThat(e1.isNotified()).isTrue();
        assertThat(e2.isNotified()).isFalse(); // second person NOT notified
    }

    @Test
    void register_organizerOnOtherOrganizerEvent_succeeds() {
        User otherOrganizer = User.builder().id(5L).email("other@test.com")
                .role(UserRole.ORGANIZER).build();
        mockSecurityAs(otherOrganizer);

        // event is owned by 'organizer' (id=1), current user is otherOrganizer (id=5)
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(5L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(0L);

        Registration saved = Registration.builder()
                .id(10L).user(otherOrganizer).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(1).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved))
                .thenReturn(RegistrationDTO.builder().id(10L).attendeeCount(1).build());

        BookingRequest req = new BookingRequest();
        assertThatCode(() -> registrationService.register(1L, req))
                .doesNotThrowAnyException();
    }

    @Test
    void register_organizerOnOwnEvent_throwsBadRequest() {
        // organizer (id=1) tries to register for event they created
        mockSecurityAs(organizer);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        // event.createdBy == organizer (id=1) → should be rejected

        BookingRequest req = new BookingRequest();
        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot register for your own event");
    }
}