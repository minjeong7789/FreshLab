package com.freshlab.freshdoctor.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsSignedJwtWithUserClaimsAndExpiration() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-07-17T03:00:00Z"), ZoneOffset.UTC);
        JwtTokenProvider provider = new JwtTokenProvider(
                "01234567890123456789012345678901",
                3600L,
                objectMapper,
                clock
        );
        User user = new User();
        user.setUserId(7L);
        user.setEmail("user@example.com");

        String token = provider.createAccessToken(user);

        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);
        JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload.get("sub").asText()).isEqualTo("7");
        assertThat(payload.get("email").asText()).isEqualTo("user@example.com");
        assertThat(payload.get("iat").asLong()).isEqualTo(1784257200L);
        assertThat(payload.get("exp").asLong()).isEqualTo(1784260800L);
        assertThat(parts[2]).isNotBlank();
        assertThat(provider.getUserId(token)).isEqualTo(7L);
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenProvider provider = providerAt("2026-07-17T03:00:00Z", 3600L);
        User user = new User();
        user.setUserId(7L);
        user.setEmail("user@example.com");
        String token = provider.createAccessToken(user);

        assertThatThrownBy(() -> provider.getUserId(token + "changed"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider issuer = providerAt("2026-07-17T03:00:00Z", 60L);
        User user = new User();
        user.setUserId(7L);
        user.setEmail("user@example.com");
        String token = issuer.createAccessToken(user);
        JwtTokenProvider verifier = providerAt("2026-07-17T03:02:00Z", 60L);

        assertThatThrownBy(() -> verifier.getUserId(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    private JwtTokenProvider providerAt(String instant, long expirationSeconds) {
        return new JwtTokenProvider(
                "01234567890123456789012345678901",
                expirationSeconds,
                objectMapper,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
        );
    }
}
