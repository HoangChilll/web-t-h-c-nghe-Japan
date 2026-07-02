package com.nihongolisten.common.security;

import com.nihongolisten.common.config.JwtProperties;
import com.nihongolisten.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final JwtProperties props;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user, TYPE_ACCESS, Duration.ofMinutes(props.accessExpiryMin()));
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, TYPE_REFRESH, Duration.ofDays(props.refreshExpiryDays()));
    }

    public long accessExpirySeconds() {
        return Duration.ofMinutes(props.accessExpiryMin()).toSeconds();
    }

    private String buildToken(User user, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Trả về claims nếu token hợp lệ và đúng loại (access/refresh), ngược lại empty. */
    public Optional<Claims> parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
