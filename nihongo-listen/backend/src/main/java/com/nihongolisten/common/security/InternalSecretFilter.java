package com.nihongolisten.common.security;

import com.nihongolisten.common.config.CrawlerProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;

/**
 * Bảo vệ các endpoint /internal/** — chỉ crawler service (giữ shared secret) được gọi.
 */
@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Secret";

    private final CrawlerProperties props;

    public InternalSecretFilter(CrawlerProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null || !MessageDigest.isEqual(Utf8.encode(provided), Utf8.encode(props.internalSecret()))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"invalid internal secret\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
