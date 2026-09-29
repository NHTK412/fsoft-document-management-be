package com.example.document_management.service;

import com.example.document_management.dto.request.ChangePasswordRequest;
import com.example.document_management.dto.request.UpdateUserProfileRequest;
import com.example.document_management.dto.response.UserProfileResponse;
import com.example.document_management.dto.response.UserSessionResponse;
import com.example.document_management.entity.User;
import com.example.document_management.entity.UserSession;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.UserRepository;
import com.example.document_management.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("testuser@example.com")
                .fullName("Nguyen Van A")
                .password("encoded_pass")
                .role(UserRoleEnum.ROLE_ADMIN)
                .title("Lead Solution Architect")
                .phone("+84 987 654 321")
                .avatarUrl("https://example.com/avatar.png")
                .isActive(true)
                .build();
    }

    @Test
    void testGetUserProfile_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile("testuser@example.com");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals("Lead Solution Architect", response.getTitle());
        assertEquals("testuser@example.com", response.getEmail());
        assertEquals("+84 987 654 321", response.getPhone());
        assertEquals("Project Admin", response.getRole());
        assertEquals("NA", response.getInitials());
        assertEquals("https://example.com/avatar.png", response.getAvatarUrl());
    }

    @Test
    void testUpdateUserProfile_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                .fullName("Tran Van B")
                .title("Senior Developer")
                .phone("+84 123 456 789")
                .build();

        UserProfileResponse response = userService.updateUserProfile("testuser@example.com", request);

        assertNotNull(response);
        assertEquals("Tran Van B", response.getFullName());
        assertEquals("Senior Developer", response.getTitle());
        assertEquals("+84 123 456 789", response.getPhone());
        assertEquals("TB", response.getInitials());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testChangePassword_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_password", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.encode("new_password_123")).thenReturn("new_encoded_pass");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_password")
                .newPassword("new_password_123")
                .build();

        assertDoesNotThrow(() -> userService.changePassword("testuser@example.com", request));

        assertEquals("new_encoded_pass", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testChangePassword_WrongCurrentPassword() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "encoded_pass")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrong_password")
                .newPassword("new_password_123")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changePassword("testuser@example.com", request));

        assertEquals("Mật khẩu hiện tại không chính xác!", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_ShortNewPassword() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_password", "encoded_pass")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("old_password")
                .newPassword("12345")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changePassword("testuser@example.com", request));

        assertEquals("Mật khẩu mới phải từ 6 ký tự trở lên!", ex.getMessage());
    }

    @Test
    void testGetUserSessions_CreatesInitialSessionWhenEmpty() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByUserIdOrderByLastActiveDesc(1L)).thenReturn(new ArrayList<>());
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36");
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("14.232.84.10");

        when(userSessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> {
            UserSession s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        List<UserSessionResponse> sessions = userService.getUserSessions("testuser@example.com", httpServletRequest);

        assertNotNull(sessions);
        assertEquals(1, sessions.size());
        assertEquals(10L, sessions.get(0).getId());
        assertEquals("14.232.84.10", sessions.get(0).getIp());
        assertTrue(sessions.get(0).getIsCurrent());
        assertEquals("desktop", sessions.get(0).getDeviceType());
        assertTrue(sessions.get(0).getDeviceName().contains("Windows PC"));
    }

    @Test
    void testRevokeSession_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        UserSession session = UserSession.builder().id(5L).user(user).build();
        when(userSessionRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(session));

        assertDoesNotThrow(() -> userService.revokeSession("testuser@example.com", 5L));

        verify(userSessionRepository, times(1)).delete(session);
    }

    @Test
    void testRevokeSession_NotFound() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));
        when(userSessionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                userService.revokeSession("testuser@example.com", 99L));
    }

    @Test
    void testRevokeAllOtherSessions_Success() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));

        UserSession current = UserSession.builder().id(1L).user(user).isCurrent(true).build();
        UserSession other = UserSession.builder().id(2L).user(user).isCurrent(false).build();
        when(userSessionRepository.findByUserIdOrderByLastActiveDesc(1L)).thenReturn(List.of(current, other));

        assertDoesNotThrow(() -> userService.revokeAllOtherSessions("testuser@example.com", httpServletRequest));

        verify(userSessionRepository, times(1)).deleteByUserIdAndIdNot(1L, 1L);
    }
}
