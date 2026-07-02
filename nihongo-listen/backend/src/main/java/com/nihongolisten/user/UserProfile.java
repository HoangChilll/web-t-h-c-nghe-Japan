package com.nihongolisten.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "jlpt_target")
    private String jlptTarget;

    @Column(name = "daily_goal_minutes", nullable = false)
    private int dailyGoalMinutes = 15;

    public UserProfile(Long userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
    }
}
