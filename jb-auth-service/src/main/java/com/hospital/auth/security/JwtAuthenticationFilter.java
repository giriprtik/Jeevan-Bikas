package com.hospital.auth.security;

import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Two ways in:
 *  1. Internal service calls to /users/resolve* carrying X-Internal-Secret get ROLE_SYSTEM.
 *  2. Everyone else: Bearer JWT, re-checked against the database so disabled accounts lose access immediately.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String INTERNAL_HEADER = "X-Internal-Secret";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final byte[] clusterSecret;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserAccountRepository userAccountRepository,
                                   @Value("${app.internal-secret}") String clusterSecret) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userAccountRepository = userAccountRepository;
        this.clusterSecret = clusterSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String internal = req.getHeader(INTERNAL_HEADER);
        if (internal != null && req.getServletPath().startsWith("/api/v1/auth/users/resolve")) {
            if (MessageDigest.isEqual(clusterSecret, internal.getBytes(StandardCharsets.UTF_8))) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        "INTERNAL_SYSTEM", null, List.of(new SimpleGrantedAuthority("ROLE_SYSTEM"))));
            } else {
                log.warn("Rejected internal call with a bad {} header", INTERNAL_HEADER);
            }
            chain.doFilter(req, res);
            return;
        }

        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtTokenProvider.parse(header.substring(7));
                userAccountRepository.findByUsername(claims.getSubject())
                        .filter(u -> u.isEnabled())
                        .ifPresent(user -> {
                            var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Invalid JWT: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }
}
