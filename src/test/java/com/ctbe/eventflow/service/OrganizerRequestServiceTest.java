package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.OrganizerRequestForm;
import com.ctbe.eventflow.dto.request.ReviewRequest;
import com.ctbe.eventflow.dto.response.OrganizerRequestDTO;
import com.ctbe.eventflow.exception.*;
import com.ctbe.eventflow.mapper.OrganizerRequestMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizerRequestServiceTest {

    @Mock OrganizerRequestRepository requestRepository;
    @Mock UserRepository             userRepository;
    @Mock OrganizerRequestMapper     requestMapper;
    @Mock EmailService               emailService;

    @InjectMocks OrganizerRequestService service;

    private User attendee;
    private User staff;
    private OrganizerRequestForm validForm;

    @BeforeEach
    void setUp() {
        attendee = User.builder().id(1L).name("Alice").email("alice@test.com")
                .role(UserRole.ATTENDEE).build();
        staff = User.builder().id(2L).name("StaffMember").email("staff@test.com")
                .role(UserRole.STAFF).build();

        validForm = new OrganizerRequestForm();
        validForm.setName("Alice Organizer");
        validForm.setEmail("alice@test.com");
        validForm.setPhone("+251911000000");
        validForm.setMessage("I want to organize tech events in Addis Ababa because I have experience running meetups.");
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

    // ── submit ────────────────────────────────────────────────

    @Test
    void submit_validAttendee_savesAndNotifiesStaff() {
        mockSecurityAs(attendee);
        when(requestRepository.existsByUserAndStatus(attendee, RequestStatus.PENDING)).thenReturn(false);
        when(userRepository.findByRole(UserRole.STAFF)).thenReturn(List.of(staff));

        OrganizerRequest saved = OrganizerRequest.builder().id(1L).user(attendee)
                .name(validForm.getName()).email(validForm.getEmail())
                .phone(validForm.getPhone()).message(validForm.getMessage())
                .status(RequestStatus.PENDING).build();
        when(requestRepository.save(any())).thenReturn(saved);
        when(requestMapper.toDTO(saved)).thenReturn(OrganizerRequestDTO.builder().id(1L).build());

        OrganizerRequestDTO result = service.submit(validForm);

        assertThat(result.getId()).isEqualTo(1L);
        verify(requestRepository).save(any(OrganizerRequest.class));
        verify(emailService).sendOrganizerRequestNotificationToStaff(
                eq(staff.getEmail()), eq(staff.getName()), eq(saved));
    }

    @Test
    void submit_notAttendee_throwsBadRequest() {
        attendee.setRole(UserRole.ORGANIZER);
        mockSecurityAs(attendee);

        assertThatThrownBy(() -> service.submit(validForm))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only attendees");
    }

    @Test
    void submit_pendingRequestAlreadyExists_throwsConflict() {
        mockSecurityAs(attendee);
        when(requestRepository.existsByUserAndStatus(attendee, RequestStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(validForm))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have a pending");
    }

    @Test
    void submit_notifiesMultipleStaffMembers() {
        mockSecurityAs(attendee);
        when(requestRepository.existsByUserAndStatus(attendee, RequestStatus.PENDING)).thenReturn(false);
        User staff2 = User.builder().id(3L).name("Staff2").email("staff2@test.com")
                .role(UserRole.STAFF).build();
        when(userRepository.findByRole(UserRole.STAFF)).thenReturn(List.of(staff, staff2));

        OrganizerRequest saved = OrganizerRequest.builder().id(1L).user(attendee)
                .name(validForm.getName()).status(RequestStatus.PENDING).build();
        when(requestRepository.save(any())).thenReturn(saved);
        when(requestMapper.toDTO(saved)).thenReturn(OrganizerRequestDTO.builder().build());

        service.submit(validForm);

        verify(emailService, times(2))
                .sendOrganizerRequestNotificationToStaff(any(), any(), any());
    }

    // ── review: approve ───────────────────────────────────────

    @Test
    void review_approve_promotesUserAndSendsEmail() {
        mockSecurityAs(staff);
        OrganizerRequest req = OrganizerRequest.builder().id(1L).user(attendee)
                .status(RequestStatus.PENDING).build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requestRepository.save(req)).thenReturn(req);
        when(requestMapper.toDTO(req)).thenReturn(
                OrganizerRequestDTO.builder().status(RequestStatus.APPROVED).build());

        ReviewRequest decision = new ReviewRequest();
        decision.setDecision(RequestStatus.APPROVED);
        decision.setNote("Welcome aboard!");

        OrganizerRequestDTO result = service.review(1L, decision);

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(attendee.getRole()).isEqualTo(UserRole.ORGANIZER); // promoted
        verify(userRepository).save(attendee);
        verify(emailService).sendRequestApproved(attendee, req);
        verify(emailService, never()).sendRequestDeclined(any(), any());
    }

    @Test
    void review_decline_doesNotPromoteAndSendsDeclinedEmail() {
        mockSecurityAs(staff);
        OrganizerRequest req = OrganizerRequest.builder().id(1L).user(attendee)
                .status(RequestStatus.PENDING).build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requestRepository.save(req)).thenReturn(req);
        when(requestMapper.toDTO(req)).thenReturn(
                OrganizerRequestDTO.builder().status(RequestStatus.DECLINED).build());

        ReviewRequest decision = new ReviewRequest();
        decision.setDecision(RequestStatus.DECLINED);
        decision.setNote("Not enough information.");

        service.review(1L, decision);

        assertThat(attendee.getRole()).isEqualTo(UserRole.ATTENDEE); // NOT promoted
        verify(userRepository, never()).save(attendee);
        verify(emailService).sendRequestDeclined(attendee, req);
        verify(emailService, never()).sendRequestApproved(any(), any());
    }

    @Test
    void review_alreadyReviewed_throwsBadRequest() {
        mockSecurityAs(staff);
        OrganizerRequest req = OrganizerRequest.builder().id(1L).user(attendee)
                .status(RequestStatus.APPROVED).build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));

        ReviewRequest decision = new ReviewRequest();
        decision.setDecision(RequestStatus.DECLINED);

        assertThatThrownBy(() -> service.review(1L, decision))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been approved");
    }

    @Test
    void review_decisionPending_throwsBadRequest() {
        mockSecurityAs(staff);
        OrganizerRequest req = OrganizerRequest.builder().id(1L).user(attendee)
                .status(RequestStatus.PENDING).build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));

        ReviewRequest decision = new ReviewRequest();
        decision.setDecision(RequestStatus.PENDING); // invalid

        assertThatThrownBy(() -> service.review(1L, decision))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("APPROVED or DECLINED");
    }

    @Test
    void review_requestNotFound_throwsResourceNotFound() {
        mockSecurityAs(staff);
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        ReviewRequest decision = new ReviewRequest();
        decision.setDecision(RequestStatus.APPROVED);

        assertThatThrownBy(() -> service.review(99L, decision))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void review_setsReviewedByAndTimestamp() {
        mockSecurityAs(staff);
        OrganizerRequest req = OrganizerRequest.builder().id(1L).user(attendee)
                .status(RequestStatus.PENDING).build();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));
        when(requestRepository.save(req)).thenReturn(req);
        when(requestMapper.toDTO(req)).thenReturn(OrganizerRequestDTO.builder().build());

        ReviewRequest decision = new ReviewRequest();
        decision.setDecision(RequestStatus.DECLINED);

        service.review(1L, decision);

        assertThat(req.getReviewedBy()).isEqualTo(staff);
        assertThat(req.getReviewedAt()).isNotNull();
    }
}