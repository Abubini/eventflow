package com.ctbe.eventflow.security;
import com.ctbe.eventflow.repository.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String token=resolveToken(request);
        if (token!=null && jwtUtils.validateToken(token) && !tokenBlacklistRepository.existsByToken(token)) {
            String email=jwtUtils.getEmailFromToken(token);
            UserDetails ud=userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(ud,null,ud.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request,response);
    }
    private String resolveToken(HttpServletRequest req) {
        String bearer=req.getHeader("Authorization");
        if (StringUtils.hasText(bearer)&&bearer.startsWith("Bearer ")) return bearer.substring(7);
        return null;
    }
}
