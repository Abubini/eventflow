package com.ctbe.eventflow.security;

import com.ctbe.eventflow.model.User;
import com.ctbe.eventflow.model.UserRole;
import com.ctbe.eventflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks CustomUserDetailsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("alice@test.com")
                .passwordHash("$2a$hashed").role(UserRole.ATTENDEE).active(true).build();
    }

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice@test.com");

        assertThat(details.getUsername()).isEqualTo("alice@test.com");
        assertThat(details.getPassword()).isEqualTo("$2a$hashed");
    }

    @Test
    void loadUserByUsername_activeUser_isEnabled() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice@test.com").isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsername_inactiveUser_isDisabled() {
        user.setActive(false);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice@test.com").isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_attendeeRole_hasAttendeeAuthority() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice@test.com").getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ATTENDEE"));
    }

    @Test
    void loadUserByUsername_organizerRole_hasOrganizerAuthority() {
        user.setRole(UserRole.ORGANIZER);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice@test.com").getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ORGANIZER"));
    }

    @Test
    void loadUserByUsername_staffRole_hasStaffAuthority() {
        user.setRole(UserRole.STAFF);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice@test.com").getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
    }

    @Test
    void loadUserByUsername_hasExactlyOneAuthority() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice@test.com").getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@test.com");
    }

    @Test
    void loadUserByUsername_accountNonExpired() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        assertThat(service.loadUserByUsername("alice@test.com").isAccountNonExpired()).isTrue();
    }

    @Test
    void loadUserByUsername_accountNonLocked() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        assertThat(service.loadUserByUsername("alice@test.com").isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_credentialsNonExpired() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        assertThat(service.loadUserByUsername("alice@test.com").isCredentialsNonExpired()).isTrue();
    }
}