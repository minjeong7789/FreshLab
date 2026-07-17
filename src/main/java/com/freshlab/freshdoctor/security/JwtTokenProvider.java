package com.freshlab.freshdoctor.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.freshlab.freshdoctor.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.security.MessageDigest;

@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] secret;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(
            @Value("${jwt.secret:${JWT_SECRET:local-development-secret-key-change-me}}") String secret,
            @Value("${jwt.access-token-expiration-seconds:${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS:3600}}") long expirationSeconds,
            ObjectMapper objectMapper
    ) {
        this(secret, expirationSeconds, objectMapper, Clock.systemUTC());
    }

    JwtTokenProvider(String secret, long expirationSeconds, ObjectMapper objectMapper, Clock clock) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes.");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        Instant issuedAt = clock.instant();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getUserId().toString());
        claims.put("email", user.getEmail());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", issuedAt.plusSeconds(expirationSeconds).getEpochSecond());

        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encodeJson(claims);
        String unsignedToken = header + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public Long getUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidTokenException();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            byte[] expectedSignature = Base64.getUrlDecoder().decode(sign(unsignedToken));
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new InvalidTokenException();
            }

            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!"HS256".equals(header.path("alg").asText())) {
                throw new InvalidTokenException();
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long expiresAt = payload.path("exp").asLong(0);
            if (expiresAt <= clock.instant().getEpochSecond()) {
                throw new InvalidTokenException();
            }
            String subject = payload.path("sub").asText();
            if (subject.isBlank()) {
                throw new InvalidTokenException();
            }
            return Long.valueOf(subject);
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException();
        }
    }

    private String encodeJson(Object value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JWT 생성에 실패했습니다.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 서명에 실패했습니다.", exception);
        }
    }
}
