package com.example.document_management.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.entity.User;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::mapToUserResponse).toList();
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        return mapToUserResponse(user);
    }

    public UserResponse changeRole(Long userId, UserRoleEnum role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        user.setRole(role);
        return mapToUserResponse(userRepository.save(user));
    }

    public UserResponse changeStatus(Long userId, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("Không thể tự khóa tài khoản của chính mình!");
        }
        user.setActive(!user.isActive());
        return mapToUserResponse(userRepository.save(user));
    }

    public void deleteUser(Long userId, String currentAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));
        if (user.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new IllegalArgumentException("Không thể tự xóa tài khoản của chính mình!");
        }
        userRepository.delete(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.isActive())
                .build();
    }
}
