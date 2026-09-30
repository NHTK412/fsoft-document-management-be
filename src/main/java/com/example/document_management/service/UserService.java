package com.example.document_management.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.document_management.dto.request.ChangePasswordRequest;
import com.example.document_management.dto.request.UpdateUserProfileRequest;
import com.example.document_management.dto.response.UserProfileResponse;
import com.example.document_management.dto.response.UserResponse;
import com.example.document_management.dto.response.UserSessionResponse;
import com.example.document_management.entity.User;
import com.example.document_management.entity.UserSession;
import com.example.document_management.enums.UserRoleEnum;
import com.example.document_management.exception.ResourceNotFoundException;
import com.example.document_management.repository.UserRepository;
import com.example.document_management.repository.UserSessionRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::mapToUserResponse);
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

    // -------------------------------------------------------------
    // USER PROFILE ENDPOINTS (Screen 3.8: UserProfile.jsx)
    // -------------------------------------------------------------

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return mapToUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateUserProfile(String email, UpdateUserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getTitle() != null) {
            user.setTitle(request.getTitle().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        user = userRepository.save(user);
        return mapToUserProfileResponse(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }

        if (request.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải từ 6 ký tự trở lên!");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public List<UserSessionResponse> getUserSessions(String email, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<UserSession> sessions = userSessionRepository.findByUserIdOrderByLastActiveDesc(user.getId());

        if (sessions.isEmpty()) {
            UserSession initialSession = UserSession.builder()
                    .user(user)
                    .deviceName(extractDeviceName(httpRequest))
                    .ipAddress(extractClientIp(httpRequest))
                    .location("Hà Nội, Việt Nam")
                    .deviceType(extractDeviceType(httpRequest))
                    .isCurrent(true)
                    .lastActive(Instant.now())
                    .createdAt(Instant.now())
                    .build();
            initialSession = userSessionRepository.save(initialSession);
            sessions = List.of(initialSession);
        } else {
            boolean hasCurrent = sessions.stream().anyMatch(UserSession::isCurrent);
            if (!hasCurrent) {
                UserSession mostRecent = sessions.get(0);
                mostRecent.setCurrent(true);
                userSessionRepository.save(mostRecent);
            }
        }

        return sessions.stream()
                .map(this::mapToUserSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(String email, Long sessionId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        UserSession session = userSessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đăng nhập với ID: " + sessionId));

        userSessionRepository.delete(session);
    }

    @Transactional
    public void revokeAllOtherSessions(String email, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<UserSession> sessions = userSessionRepository.findByUserIdOrderByLastActiveDesc(user.getId());
        if (sessions.isEmpty()) {
            return;
        }

        UserSession current = sessions.stream()
                .filter(UserSession::isCurrent)
                .findFirst()
                .orElse(sessions.get(0));

        userSessionRepository.deleteByUserIdAndIdNot(user.getId(), current.getId());
    }

    @Transactional
    public UserProfileResponse uploadAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp hình ảnh hợp lệ!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận các tệp hình ảnh (JPEG, PNG, WebP, GIF, SVG)!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (user.getAvatarUrl() != null && user.getAvatarUrl().startsWith("avatars/")) {
            try {
                storageService.deleteFile(user.getAvatarUrl());
            } catch (Exception e) {
                log.warn("Không thể xóa ảnh đại diện cũ {}: {}", user.getAvatarUrl(), e.getMessage());
            }
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        String s3Key = "avatars/user_" + user.getId() + "_" + System.currentTimeMillis() + "." + extension;
        storageService.uploadFile(file, s3Key);

        user.setAvatarUrl(s3Key);
        userRepository.save(user);

        return mapToUserProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse removeAvatar(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (user.getAvatarUrl() != null && user.getAvatarUrl().startsWith("avatars/")) {
            try {
                storageService.deleteFile(user.getAvatarUrl());
            } catch (Exception e) {
                log.warn("Không thể xóa ảnh đại diện {}: {}", user.getAvatarUrl(), e.getMessage());
            }
        }

        user.setAvatarUrl(null);
        userRepository.save(user);

        return mapToUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public InputStream getAvatarStream(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            throw new ResourceNotFoundException("Người dùng chưa có ảnh đại diện.");
        }

        return storageService.getFile(user.getAvatarUrl());
    }

    @Transactional(readOnly = true)
    public String getAvatarContentType(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với id: " + userId));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            return "image/jpeg";
        }

        String lower = user.getAvatarUrl().toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }

    // -------------------------------------------------------------
    // HELPER METHODS
    // -------------------------------------------------------------

    private UserProfileResponse mapToUserProfileResponse(User user) {
        String roleDisplay = (user.getRole() == UserRoleEnum.ROLE_ADMIN) ? "Project Admin" : "Member";
        String initials = extractInitials(user.getFullName());

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .title(user.getTitle())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(roleDisplay)
                .initials(initials)
                .avatarUrl(user.getAvatarDisplayUrl())
                .build();
    }

    private UserSessionResponse mapToUserSessionResponse(UserSession s) {
        return UserSessionResponse.builder()
                .id(s.getId())
                .deviceName(s.getDeviceName())
                .location(s.getLocation() != null ? s.getLocation() : "Việt Nam")
                .ip(s.getIpAddress())
                .isCurrent(s.isCurrent())
                .deviceType(s.getDeviceType() != null ? s.getDeviceType() : "desktop")
                .lastActive(s.getLastActive() != null
                        ? DateTimeFormatter.ISO_INSTANT.format(s.getLastActive())
                        : DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                .build();
    }

    private String extractInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            String first = parts[0].substring(0, 1).toUpperCase();
            String last = parts[parts.length - 1].substring(0, 1).toUpperCase();
            return first + last;
        }
        return parts[0].substring(0, 1).toUpperCase();
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "localhost".equalsIgnoreCase(ip)) {
            ip = "127.0.0.1";
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return (ip != null && !ip.isBlank()) ? ip : "127.0.0.1";
    }

    private String extractDeviceName(HttpServletRequest request) {
        if (request == null) return "Windows PC - Web Browser";
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) return "Windows PC - Web Browser";

        String os = "Unknown OS";
        if (userAgent.contains("Windows")) os = "Windows PC";
        else if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS")) os = "MacBook / macOS";
        else if (userAgent.contains("iPhone")) os = "Apple iPhone";
        else if (userAgent.contains("iPad")) os = "Apple iPad";
        else if (userAgent.contains("Android")) os = "Android Device";
        else if (userAgent.contains("Linux")) os = "Linux PC";

        String browser = "Web Browser";
        if (userAgent.contains("Edg/")) browser = "Microsoft Edge";
        else if (userAgent.contains("Chrome/")) browser = "Google Chrome";
        else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) browser = "Safari";
        else if (userAgent.contains("Firefox/")) browser = "Mozilla Firefox";

        return os + " - " + browser;
    }

    private String extractDeviceType(HttpServletRequest request) {
        if (request == null) return "desktop";
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "desktop";
        String lower = userAgent.toLowerCase();
        if (lower.contains("ipad") || lower.contains("tablet")) return "tablet";
        if (lower.contains("mobile") || lower.contains("iphone") || lower.contains("android")) return "mobile";
        return "desktop";
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isActive(user.isActive())
                .avatarUrl(user.getAvatarDisplayUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
