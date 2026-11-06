package com.hospital.auth;

import com.hospital.auth.support.TestJwt;
import com.hospital.auth.util.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(TestJwt.SECRET, 60_000);

    @Test
    void roundTripsTheClaimsTheOtherServicesRelyOn() {
        var claims = provider.parse(provider.generateToken("labtech", "LABTECH", "Bikash Rai"));

        assertThat(claims.getSubject()).isEqualTo("labtech");
        assertThat(claims.get("roles", String.class)).isEqualTo("LABTECH");
        assertThat(claims.get("name", String.class)).isEqualTo("Bikash Rai");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void aTamperedTokenIsRejected() {
        String token = provider.generateToken("nurse", "NURSE", "Sita Thapa");
        String[] parts = token.split("\\.");
        // Swap the payload for one that claims ADMIN, keep the original signature.
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"nurse\",\"roles\":\"ADMIN\"}".getBytes());
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> provider.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenFromAnotherKeyIsRejected() {
        String foreign = new JwtTokenProvider("some-other-secret-that-is-also-long-enough-xx", 60_000)
                .generateToken("nurse", "NURSE", "x");

        assertThatThrownBy(() -> provider.parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void anExpiredTokenIsRejected() {
        String token = new JwtTokenProvider(TestJwt.SECRET, -1_000).generateToken("nurse", "NURSE", "x");

        assertThatThrownBy(() -> provider.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void aSecretShorterThan256BitsIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtTokenProvider("too-short", 60_000)).isInstanceOf(WeakKeyException.class);
    }
}
