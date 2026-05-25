package com.ctbe.eventflow.service;
import com.ctbe.eventflow.dto.request.*;
import com.ctbe.eventflow.dto.response.UserDTO;
import com.ctbe.eventflow.exception.ResourceNotFoundException;
import com.ctbe.eventflow.mapper.UserMapper;
import com.ctbe.eventflow.model.User;
import com.ctbe.eventflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    @Transactional(readOnly=true) public UserDTO getProfile() { return userMapper.toDTO(currentUser()); }
    @Transactional
    public UserDTO updateProfile(UpdateProfileRequest req) {
        User user=currentUser(); user.setName(req.getName()); return userMapper.toDTO(userRepository.save(user));
    }
    @Transactional(readOnly=true)
    public Page<UserDTO> listAll(Pageable pageable) { return userRepository.findAll(pageable).map(userMapper::toDTO); }
    @Transactional
    public UserDTO updateRole(Long userId,RoleUpdateRequest req) {
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found: "+userId));
        user.setRole(req.getRole()); return userMapper.toDTO(userRepository.save(user));
    }
    private User currentUser() {
        String email=SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(()->new ResourceNotFoundException("User not found"));
    }
}
