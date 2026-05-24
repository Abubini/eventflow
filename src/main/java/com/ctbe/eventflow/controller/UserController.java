package com.ctbe.eventflow.controller;
import com.ctbe.eventflow.dto.request.*;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping("/api/users/me") public ResponseEntity<UserDTO> getProfile() { return ResponseEntity.ok(userService.getProfile()); }
    @PutMapping("/api/users/me") public ResponseEntity<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileRequest req) { return ResponseEntity.ok(userService.updateProfile(req)); }
    @GetMapping("/api/admin/users") @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Page<UserDTO>> listUsers(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok(userService.listAll(PageRequest.of(page,Math.min(size,100))));
    }
    @PatchMapping("/api/admin/users/{id}/role") @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<UserDTO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest req) {
        return ResponseEntity.ok(userService.updateRole(id,req));
    }
}
