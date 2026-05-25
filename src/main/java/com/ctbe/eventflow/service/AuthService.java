package com.ctbe.eventflow.service;
import com.ctbe.eventflow.dto.request.*;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.exception.ConflictException;
import com.ctbe.eventflow.mapper.UserMapper;
import com.ctbe.eventflow.model.*;
import com.ctbe.eventflow.repository.*;
import com.ctbe.eventflow.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final UserMapper userMapper;
    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) throw new ConflictException("Email already registered: "+request.getEmail());
        User user=User.builder().name(request.getName()).email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword())).role(UserRole.ATTENDEE).active(true).build();
        return userMapper.toDTO(userRepository.save(user));
    }
    @Transactional(readOnly=true)
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
        User user=userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token=jwtUtils.generateToken(user.getEmail());
        return TokenResponse.builder().token(token).type("Bearer").expiresIn(jwtUtils.getExpirationMs()).user(userMapper.toDTO(user)).build();
    }
    @Transactional
    public void logout(String token) {
        tokenBlacklistRepository.save(TokenBlacklist.builder().token(token).expiresAt(jwtUtils.getExpiryFromToken(token)).build());
    }
}
