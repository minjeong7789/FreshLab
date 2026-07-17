package com.freshlab.freshdoctor.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
