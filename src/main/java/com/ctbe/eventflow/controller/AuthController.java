package com.ctbe.eventflow.controller;
import com.ctbe.eventflow.dto.request.*;
import com.ctbe.eventflow.dto.response.*;
import com.ctbe.eventflow.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req) {
        String bearer=req.getHeader("Authorization");
        if (StringUtils.hasText(bearer)&&bearer.startsWith("Bearer ")) authService.logout(bearer.substring(7));
        return ResponseEntity.ok().build();
    }
}
