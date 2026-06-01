// src/test/java/com/ctbe/eventflow/service/UserServiceTest.java
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;

    @InjectMocks UserService userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Alice").email("alice@example.com")
                .role(UserRole.ATTENDEE).active(true).build();
        userDTO = UserDTO.builder().id(1L).name("Alice").email("alice@example.com")
                .role(UserRole.ATTENDEE).active(true).build();
        // Do NOT call mockSecurityAs here — not every test needs the security context
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityAs(User u) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(u.getEmail());
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ── getProfile ────────────────────────────────────────────

    @Test
    void getProfile_returnsCurrentUserDTO() {
        mockSecurityAs(user); // needs security context
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        UserDTO result = userService.getProfile();

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getProfile_userNotFound_throwsResourceNotFound() {
        mockSecurityAs(user); // needs security context
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateProfile ─────────────────────────────────────────

    @Test
    void updateProfile_changesName() {
        mockSecurityAs(user); // needs security context
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Alice Updated");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(UserDTO.builder().name("Alice Updated").build());

        UserDTO result = userService.updateProfile(req);

        assertThat(result.getName()).isEqualTo("Alice Updated");
        verify(userRepository).save(user);
    }

    // ── listAll ───────────────────────────────────────────────

    @Test
    void listAll_returnsPageOfUsers() {
        // No security context needed — listAll doesn't call currentUser()
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        Page<UserDTO> result = userService.listAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void listAll_emptyDatabase_returnsEmptyPage() {
        // No security context needed
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        Page<UserDTO> result = userService.listAll(PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    // ── updateRole ────────────────────────────────────────────

    @Test
    void updateRole_changesUserRole() {
        // No security context needed — updateRole uses userId param, not currentUser()
        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.ORGANIZER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(UserDTO.builder().role(UserRole.ORGANIZER).build());

        UserDTO result = userService.updateRole(1L, req);

        assertThat(result.getRole()).isEqualTo(UserRole.ORGANIZER);
        verify(userRepository).save(user);
    }

    @Test
    void updateRole_userNotFound_throwsResourceNotFound() {
        // No security context needed
        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setRole(UserRole.STAFF);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(99L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}