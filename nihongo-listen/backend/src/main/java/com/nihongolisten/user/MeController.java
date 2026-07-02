package com.nihongolisten.user;

import com.nihongolisten.common.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public MeController(UserRepository userRepository, UserProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User không tồn tại"));
        UserProfile profile = profileRepository.findById(userId).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("role", user.getRole().name());
        result.put("subscriptionStatus", user.getSubscriptionStatus());
        result.put("displayName", profile != null ? profile.getDisplayName() : null);
        result.put("jlptTarget", profile != null ? profile.getJlptTarget() : null);
        result.put("dailyGoalMinutes", profile != null ? profile.getDailyGoalMinutes() : 15);
        return result;
    }
}
