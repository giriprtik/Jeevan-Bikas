package com.hospital.opd.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Mints access tokens exactly like the auth service does (HS256, sub = username, roles = role name). */
public final class TestJwt {

    /** Shared signing key for the test context; must be at least 32 bytes for HS256. */
    public static final String SECRET = "test-only-jwt-secret-that-is-long-enough-for-hs256";
    public static final String INTERNAL_SECRET = "test-only-internal-secret";

    private TestJwt() {}

    public static String bearer(String username, String role) {
        return "Bearer " + token(username, role, SECRET, 3_600_000);
    }

    /** A token signed with a different key: must be rejected as if no token was sent. */
    public static String forged(String username, String role) {
        return "Bearer " + token(username, role, "some-other-secret-that-is-also-long-enough-xx", 3_600_000);
    }

    public static String expired(String username, String role) {
        return "Bearer " + token(username, role, SECRET, -60_000);
    }

    private static String token(String username, String role, String secret, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("roles", role)
                .claim("name", username)
                .issuedAt(new Date(now - 120_000))
                .expiration(new Date(now + ttlMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
