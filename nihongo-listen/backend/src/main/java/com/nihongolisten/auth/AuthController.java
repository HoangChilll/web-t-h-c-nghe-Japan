package com.nihongolisten.auth;

import com.nihongolisten.auth.dto.AuthResponse;
import com.nihongolisten.auth.dto.LoginRequest;
import com.nihongolisten.auth.dto.RefreshRequest;
import com.nihongolisten.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /**
     * JWT stateless — logout phía server chỉ là no-op; client xoá token.
     * TODO: thêm refresh-token blacklist trong Redis khi cần thu hồi thật sự.
     */
    @PostMapping("/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Đã đăng xuất");
    }
}
