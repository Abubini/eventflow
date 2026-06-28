package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.BookingRequest;
import com.ctbe.eventflow.dto.response.RegistrationDTO;
import com.ctbe.eventflow.dto.response.WaitlistDTO;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtendedRegistrationServiceTest {

    @Mock RegistrationRepository registrationRepository;
    @Mock WaitlistRepository     waitlistRepository;
    @Mock EventRepository        eventRepository;
    @Mock UserRepository         userRepository;
    @Mock RegistrationMapper     registrationMapper;
    @Mock UserMapper             userMapper;
    @Mock EmailService           emailService;

    @InjectMocks RegistrationService registrationService;

    private User attendee;
    private User organizer;
    private User staffUser;
    private Event event;

    @BeforeEach
    void setUp() {
        attendee  = User.builder().id(1L).email("att@test.com").role(UserRole.ATTENDEE).name("Att").build();
        organizer = User.builder().id(2L).email("org@test.com").role(UserRole.ORGANIZER).name("Org").build();
        staffUser = User.builder().id(3L).email("staff@test.com").role(UserRole.STAFF).name("Staff").build();

        event = Event.builder().id(1L).title("Test Event").location("Addis")
                .dateTime(LocalDateTime.now().plusDays(5))
                .status(EventStatus.PUBLISHED).capacity(100).createdBy(organizer).build();
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
    //  register with BookingRequest (attendeeCount)
    // ════════════════════════════════════════════════════

    @Test
    void register_defaultOneAttendee_savesWithCount1() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(0L);
        Registration saved = Registration.builder().id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(1).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(
                RegistrationDTO.builder().id(1L).attendeeCount(1).build());

        BookingRequest req = new BookingRequest();
        // attendeeCount defaults to 1
        RegistrationDTO result = registrationService.register(1L, req);

        assertThat(result.getAttendeeCount()).isEqualTo(1);
        ArgumentCaptor<Registration> cap = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(cap.capture());
        assertThat(cap.getValue().getAttendeeCount()).isEqualTo(1);
    }

    @Test
    void register_multipleAttendees_savesCorrectCount() {
        mockSecurityAs(attendee);
        event.setCapacity(100);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(90L);
        Registration saved = Registration.builder().id(1L).user(attendee).event(event)
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
    void register_requestMoreThanAvailable_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setCapacity(100);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(98L); // only 2 left

        BookingRequest req = new BookingRequest();
        req.setAttendeeCount(5);

        assertThatThrownBy(() -> registrationService.register(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only 2 seat(s) left");
    }

    @Test
    void register_sendsBookingConfirmationEmail() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(0L);
        Registration saved = Registration.builder().id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(1).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(RegistrationDTO.builder().build());

        registrationService.register(1L, new BookingRequest());

        verify(emailService).sendBookingConfirmation(saved);
    }

    // ════════════════════════════════════════════════════
    //  cancel — date enforcement
    // ════════════════════════════════════════════════════

    @Test
    void cancel_afterEventStarted_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().minusHours(1)); // past
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.cancel(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("after the event has started");
    }

    @Test
    void cancel_beforeEventStarted_deletesAndSendsEmail() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().plusDays(2));
        Registration reg = Registration.builder().id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(2).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(reg));
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of());

        registrationService.cancel(1L);

        verify(registrationRepository).delete(reg);
        verify(emailService).sendCancellationConfirmation(attendee, event, 2);
    }

    @Test
    void cancel_notifiesWaitlistWithReleasedSeatCount() {
        mockSecurityAs(attendee);
        event.setDateTime(LocalDateTime.now().plusDays(2));
        Registration reg = Registration.builder().id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(3).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(reg));

        User waiter = User.builder().id(10L).email("w@test.com").name("Waiter").build();
        WaitlistEntry entry = WaitlistEntry.builder().id(1L).user(waiter).event(event).build();
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of(entry));
        when(waitlistRepository.save(any())).thenReturn(entry);

        registrationService.cancel(1L);

        // 3 seats released, 1 waiter → 1 notification
        verify(emailService).sendSlotAvailableNotification(waiter, event);
    }

    // ════════════════════════════════════════════════════
    //  getMyRegistrations
    // ════════════════════════════════════════════════════

    @Test
    void getMyRegistrations_returnsPageForCurrentUser() {
        mockSecurityAs(attendee);
        Registration reg = Registration.builder().id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(1).build();
        when(registrationRepository.findByUserOrderByRegisteredAtDesc(eq(attendee), any()))
                .thenReturn(new PageImpl<>(List.of(reg)));
        when(registrationMapper.toDTO(reg)).thenReturn(
                RegistrationDTO.builder().id(1L).build());

        Page<RegistrationDTO> result =
                registrationService.getMyRegistrations(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void getMyRegistrations_empty_returnsEmptyPage() {
        mockSecurityAs(attendee);
        when(registrationRepository.findByUserOrderByRegisteredAtDesc(eq(attendee), any()))
                .thenReturn(Page.empty());

        assertThat(registrationService.getMyRegistrations(PageRequest.of(0, 20)).getContent())
                .isEmpty();
    }

    // ════════════════════════════════════════════════════
    //  joinWaitlist
    // ════════════════════════════════════════════════════

    @Test
    void joinWaitlist_fullEvent_savesEntry() {
        mockSecurityAs(attendee);
        event.setCapacity(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(10L); // full
        WaitlistEntry entry = WaitlistEntry.builder().id(1L).user(attendee).event(event).build();
        when(waitlistRepository.save(any())).thenReturn(entry);
        when(registrationMapper.toWaitlistDTO(entry)).thenReturn(
                WaitlistDTO.builder().id(1L).eventId(1L).build());

        WaitlistDTO result = registrationService.joinWaitlist(1L);

        assertThat(result.getEventId()).isEqualTo(1L);
        verify(waitlistRepository).save(any(WaitlistEntry.class));
    }

    @Test
    void joinWaitlist_slotsAvailable_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setCapacity(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(5L); // not full

        assertThatThrownBy(() -> registrationService.joinWaitlist(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("seats available");
    }

    @Test
    void joinWaitlist_alreadyRegistered_throwsConflict() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.joinWaitlist(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void joinWaitlist_alreadyOnWaitlist_throwsConflict() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.joinWaitlist(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already on the waitlist");
    }


    @Test
    void joinWaitlist_sendsConfirmationEmail() {
        mockSecurityAs(attendee);
        event.setCapacity(10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(waitlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(10L);
        WaitlistEntry entry = WaitlistEntry.builder().id(1L).user(attendee).event(event).build();
        when(waitlistRepository.save(any())).thenReturn(entry);
        when(registrationMapper.toWaitlistDTO(entry))
                .thenReturn(WaitlistDTO.builder().id(1L).build());

        registrationService.joinWaitlist(1L);

        verify(emailService).sendWaitlistConfirmation(attendee, event);
    }

    // ════════════════════════════════════════════════════
    //  leaveWaitlist
    // ════════════════════════════════════════════════════

    @Test
    void leaveWaitlist_existing_deletesEntry() {
        mockSecurityAs(attendee);
        WaitlistEntry entry = WaitlistEntry.builder().id(1L).user(attendee).event(event).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(waitlistRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(entry));

        registrationService.leaveWaitlist(1L);

        verify(waitlistRepository).delete(entry);
    }

    @Test
    void leaveWaitlist_notOnWaitlist_throwsResourceNotFound() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(waitlistRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.leaveWaitlist(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not on the waitlist");
    }

    // ════════════════════════════════════════════════════
    //  notifyWaitlist
    // ════════════════════════════════════════════════════

    @Test
    void notifyWaitlist_nullCapacity_doesNothing() {
        event.setCapacity(null);

        registrationService.notifyWaitlist(event, 5);

        verify(waitlistRepository, never()).findPendingByEvent(any());
    }

    @Test
    void notifyWaitlist_noPendingEntries_doesNothing() {
        event.setCapacity(100);
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of());

        registrationService.notifyWaitlist(event, 3);

        verify(emailService, never()).sendSlotAvailableNotification(any(), any());
    }

    @Test
    void notifyWaitlist_notifiesUpToReleasedCount() {
        event.setCapacity(100);
        User w1 = User.builder().id(10L).email("w1@t.com").name("W1").build();
        User w2 = User.builder().id(11L).email("w2@t.com").name("W2").build();
        User w3 = User.builder().id(12L).email("w3@t.com").name("W3").build();
        WaitlistEntry e1 = WaitlistEntry.builder().id(1L).user(w1).event(event).notified(false).build();
        WaitlistEntry e2 = WaitlistEntry.builder().id(2L).user(w2).event(event).notified(false).build();
        WaitlistEntry e3 = WaitlistEntry.builder().id(3L).user(w3).event(event).notified(false).build();
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of(e1, e2, e3));
        when(waitlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        registrationService.notifyWaitlist(event, 2); // only 2 seats freed

        verify(emailService, times(2)).sendSlotAvailableNotification(any(), eq(event));
        assertThat(e1.isNotified()).isTrue();
        assertThat(e2.isNotified()).isTrue();
        assertThat(e3.isNotified()).isFalse(); // third person not notified
    }

    @Test
    void notifyWaitlist_marksEntryAsNotified() {
        event.setCapacity(10);
        User waiter = User.builder().id(10L).email("w@t.com").name("W").build();
        WaitlistEntry entry = WaitlistEntry.builder().id(1L).user(waiter).event(event).notified(false).build();
        when(waitlistRepository.findPendingByEvent(event)).thenReturn(List.of(entry));
        when(waitlistRepository.save(entry)).thenReturn(entry);

        registrationService.notifyWaitlist(event, 1);

        assertThat(entry.isNotified()).isTrue();
        verify(waitlistRepository).save(entry);
    }

    // ════════════════════════════════════════════════════
    //  adminRegister (staff manually registers a user)
    // ════════════════════════════════════════════════════

    @Test
    void adminRegister_happyPath_savesRegistration() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(0L);
        Registration saved = Registration.builder().id(1L).user(attendee).event(event)
                .status(RegStatus.CONFIRMED).attendeeCount(1).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(RegistrationDTO.builder().id(1L).build());

        RegistrationDTO result = registrationService.adminRegister(1L, 1L, new BookingRequest());

        assertThat(result.getId()).isEqualTo(1L);
        verify(registrationRepository).save(any());
    }

    @Test
    void adminRegister_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.adminRegister(1L, 99L, new BookingRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void adminRegister_alreadyRegistered_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.adminRegister(1L, 1L, new BookingRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void adminRegister_fullCapacity_throwsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(attendee));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(registrationRepository.sumAttendeeCountByEventAndStatus(event, RegStatus.CONFIRMED))
                .thenReturn(100L); // at capacity

        assertThatThrownBy(() -> registrationService.adminRegister(1L, 1L, new BookingRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("full capacity");
    }
}