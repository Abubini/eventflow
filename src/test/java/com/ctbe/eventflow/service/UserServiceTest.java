package com.ctbe.eventflow.service;

import com.ctbe.eventflow.dto.request.RoleUpdateRequest;
import com.ctbe.eventflow.dto.request.UpdateProfileRequest;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.ResourceNotFoundException;
import com.ctbe.eventflow.mapper.UserMapper;
import com.ctbe.eventflow.model.User;
import com.ctbe.eventflow.model.UserRole;
import com.ctbe.eventflow.repository.UserRepository;
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
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper     userMapper;

    @InjectMocks UserService userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Alice").email("alice@test.com")
                .role(UserRole.ATTENDEE).active(true)
                .createdAt(LocalDateTime.now()).build();
        userDTO = UserDTO.builder().id(1L).name("Alice").email("alice@test.com")
                .role(UserRole.ATTENDEE).active(true).build();
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private void mockSecurityAs(User u) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(u.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ════════════════════════════════════════════════════
    //  getProfile
    // ════════════════════════════════════════════════════

    @Test
    void getProfile_returnsCurrentUserDTO() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.getProfile();

        assertThat(result.getEmail()).isEqualTo("alice@test.com");
        assertThat(result.getName()).isEqualTo("Alice");
    }

    @Test
    void getProfile_userNotFound_throwsResourceNotFound() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getProfile_callsRepositoryWithCorrectEmail() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        userService.getProfile();

        verify(userRepository).findByEmail("alice@test.com");
    }

    @Test
    void getProfile_mapsViaUserMapper() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        userService.getProfile();

        verify(userMapper).toDTO(user);
    }

    // ════════════════════════════════════════════════════
    //  updateProfile
    // ════════════════════════════════════════════════════

    @Test
    void updateProfile_changesName() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(UserDTO.builder().name("Alice Updated").build());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Alice Updated");

        UserDTO result = userService.updateProfile(req);

        assertThat(result.getName()).isEqualTo("Alice Updated");
    }

    @Test
    void updateProfile_persistsNewName() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("New Name");
        userService.updateProfile(req);

        assertThat(user.getName()).isEqualTo("New Name");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_userNotFound_throwsResourceNotFound() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Any Name");

        assertThatThrownBy(() -> userService.updateProfile(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_savesOnce() {
        mockSecurityAs(user);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Bob");
        userService.updateProfile(req);

        verify(userRepository, times(1)).save(user);
    }

    // ════════════════════════════════════════════════════
    //  listAll
    // ════════════════════════════════════════════════════

    @Test
    void listAll_returnsPageOfUsers() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        Page<UserDTO> result = userService.listAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void listAll_emptyDatabase_returnsEmptyPage() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        assertThat(userService.listAll(PageRequest.of(0, 20)).getContent()).isEmpty();
    }

    @Test
    void listAll_multipleUsers_allMapped() {
        User user2 = User.builder().id(2L).name("Bob").email("bob@test.com")
                .role(UserRole.ORGANIZER).active(true).build();
        UserDTO dto2 = UserDTO.builder().id(2L).name("Bob").build();
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user, user2)));
        when(userMapper.toDTO(user)).thenReturn(userDTO);
        when(userMapper.toDTO(user2)).thenReturn(dto2);

        Page<UserDTO> result = userService.listAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void listAll_respectsPaginationParams() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        userService.listAll(PageRequest.of(3, 5));

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(cap.capture());
        assertThat(cap.getValue().getPageNumber()).isEqualTo(3);
        assertThat(cap.getValue().getPageSize()).isEqualTo(5);
    }

    // ════════════════════════════════════════════════════
    //  updateRole
    // ════════════════════════════════════════════════════

    @Test
    void updateRole_attendeeToOrganizer_changesRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(
                UserDTO.builder().id(1L).role(UserRole.ORGANIZER).build());

        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.ORGANIZER);

        UserDTO result = userService.updateRole(1L, req);

        assertThat(result.getRole()).isEqualTo(UserRole.ORGANIZER);
        assertThat(user.getRole()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    void updateRole_attendeeToStaff_changesRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(
                UserDTO.builder().id(1L).role(UserRole.STAFF).build());

        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.STAFF);

        userService.updateRole(1L, req);

        assertThat(user.getRole()).isEqualTo(UserRole.STAFF);
    }

    @Test
    void updateRole_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.ORGANIZER);

        assertThatThrownBy(() -> userService.updateRole(99L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateRole_userNotFound_neverSaves() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.ORGANIZER);

        assertThatThrownBy(() -> userService.updateRole(99L, req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRole_savesOnce() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.ORGANIZER);
        userService.updateRole(1L, req);

        verify(userRepository, times(1)).save(user);
    }
}