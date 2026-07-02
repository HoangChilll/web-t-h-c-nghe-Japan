package com.nihongolisten.auth;

import com.nihongolisten.auth.dto.AuthResponse;
import com.nihongolisten.auth.dto.LoginRequest;
import com.nihongolisten.auth.dto.RegisterRequest;
import com.nihongolisten.common.exception.ApiException;
import com.nihongolisten.common.security.JwtService;
import com.nihongolisten.user.User;
import com.nihongolisten.user.UserProfile;
import com.nihongolisten.user.UserProfileRepository;
import com.nihongolisten.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       UserProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email đã được đăng ký");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password())));
        String displayName = request.displayName() != null && !request.displayName().isBlank()
                ? request.displayName()
                : email.substring(0, email.indexOf('@'));
        profileRepository.save(new UserProfile(user.getId(), displayName));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email hoặc mật khẩu không đúng"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email hoặc mật khẩu không đúng");
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        var claims = jwtService.parse(refreshToken, JwtService.TYPE_REFRESH)
                .orElseThrow(() -> ApiException.unauthorized("Refresh token không hợp lệ hoặc đã hết hạn"));
        User user = userRepository.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> ApiException.unauthorized("User không tồn tại"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.accessExpirySeconds(),
                user.getId(),
                user.getEmail(),
                user.getRole().name());
    }
}
