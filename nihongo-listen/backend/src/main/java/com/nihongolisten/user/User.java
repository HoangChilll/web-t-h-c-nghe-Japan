package com.nihongolisten.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.FREE;

    @Column(name = "subscription_status", nullable = false)
    private String subscriptionStatus = "NONE";

    @Column(name = "subscription_end_at")
    private Instant subscriptionEndAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
