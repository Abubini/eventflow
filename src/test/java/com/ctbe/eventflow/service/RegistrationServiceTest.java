// src/test/java/com/ctbe/eventflow/service/RegistrationServiceTest.java
package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.response.RegistrationDTO;
import com.ctbe.eventflow.dto.response.UserDTO;
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

import java.util.Optional;

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
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(5L);
        Registration saved = Registration.builder().id(10L).user(attendee).event(event).status(RegStatus.CONFIRMED).build();
        when(registrationRepository.save(any())).thenReturn(saved);
        when(registrationMapper.toDTO(saved)).thenReturn(RegistrationDTO.builder().id(10L).build());

        RegistrationDTO result = registrationService.register(1L);

        assertThat(result.getId()).isEqualTo(10L);
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    void register_eventNotFound_throwsResourceNotFound() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void register_eventNotPublished_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.register(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not open");
    }

    @Test
    void register_cancelledEvent_throwsBadRequest() {
        mockSecurityAs(attendee);
        event.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.register(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void register_alreadyRegistered_throwsConflict() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Already registered");
    }

    @Test
    void register_atFullCapacity_throwsBadRequest() {
        mockSecurityAs(attendee);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByUserIdAndEventId(2L, 1L)).thenReturn(false);
        when(registrationRepository.countByEventAndStatus(event, RegStatus.CONFIRMED)).thenReturn(100L);

        assertThatThrownBy(() -> registrationService.register(1L))
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

        assertThatCode(() -> registrationService.register(1L)).doesNotThrowAnyException();
    }

    // ── cancel ────────────────────────────────────────────────

    @Test
    void cancel_existingRegistration_deletesIt() {
        mockSecurityAs(attendee);
        Registration reg = Registration.builder().id(10L).user(attendee).event(event).build();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.findByUserAndEvent(attendee, event)).thenReturn(Optional.of(reg));

        registrationService.cancel(1L);

        verify(registrationRepository).delete(reg);
    }

    @Test
    void cancel_notRegistered_throwsResourceNotFound() {
        mockSecurityAs(attendee);
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
}