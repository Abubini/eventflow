package com.ctbe.eventflow.security;

import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtUtils jwtUtils;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock TokenBlacklistRepository tokenBlacklistRepository;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtUtils, userDetailsService, tokenBlacklistRepository);
        SecurityContextHolder.clearContext();
    }

    private UserDetails fakeUser() {
        return new User("alice@test.com", "hashed",
                List.of(new SimpleGrantedAuthority("ROLE_ATTENDEE")));
    }

    // ════════════════════════════════════════════════════
    //  Valid token — authentication is set
    // ════════════════════════════════════════════════════

    @Test
    void validToken_notBlacklisted_setsAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtUtils.validateToken("valid.jwt.token")).thenReturn(true);
        when(tokenBlacklistRepository.existsByToken("valid.jwt.token")).thenReturn(false);
        when(jwtUtils.getEmailFromToken("valid.jwt.token")).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(fakeUser());

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("alice@test.com");
    }

    @Test
    void validToken_notBlacklisted_chainsToNextFilter() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "Bearer valid.jwt.token");
        when(jwtUtils.validateToken("valid.jwt.token")).thenReturn(true);
        when(tokenBlacklistRepository.existsByToken("valid.jwt.token")).thenReturn(false);
        when(jwtUtils.getEmailFromToken("valid.jwt.token")).thenReturn("alice@test.com");
        when(userDetailsService.loadUserByUsername("alice@test.com")).thenReturn(fakeUser());

        filter.doFilter(req, resp, chain);

        assertThat(chain.getRequest()).isNotNull(); // chain was called
    }

    // ════════════════════════════════════════════════════
    //  Blacklisted token — authentication NOT set
    // ════════════════════════════════════════════════════

    @Test
    void blacklistedToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "Bearer blacklisted.token");
        when(jwtUtils.validateToken("blacklisted.token")).thenReturn(true);
        when(tokenBlacklistRepository.existsByToken("blacklisted.token")).thenReturn(true);

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    // ════════════════════════════════════════════════════
    //  Invalid token — authentication NOT set
    // ════════════════════════════════════════════════════

    @Test
    void invalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "Bearer bad.token");
        when(jwtUtils.validateToken("bad.token")).thenReturn(false);

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenBlacklistRepository, never()).existsByToken(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    // ════════════════════════════════════════════════════
    //  Missing / malformed Authorization header
    // ════════════════════════════════════════════════════

    @Test
    void noAuthorizationHeader_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils, never()).validateToken(any());
    }

    @Test
    void authHeaderWithoutBearerPrefix_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "Basic dXNlcjpwYXNz"); // Basic auth header

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils, never()).validateToken(any());
    }

    @Test
    void emptyAuthorizationHeader_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "");

        filter.doFilter(req, resp, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void bearerWithEmptyToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        req.addHeader("Authorization", "Bearer ");

        filter.doFilter(req, resp, chain);

        // Empty string after "Bearer " — validateToken should get ""
        // and return false, so auth stays null
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void allRequestsContinueFilterChain_evenWithoutToken() throws Exception {
        MockHttpServletRequest  req  = new MockHttpServletRequest();
        MockHttpServletResponse resp = new MockHttpServletResponse();
        MockFilterChain         chain = new MockFilterChain();

        filter.doFilter(req, resp, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}