package com.green.university.jwt;

import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.UserJpaRepository;
import com.green.university.repository.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final String secret;
    private final Long expiration;
    private final SecretKey signingKey;

    @Autowired
    private UserJpaRepository userJpaRepository;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") Long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();

            log.debug("Token expiration: {}", expiration);
            log.debug("Current time: {}", now);
            log.debug("Is expired: {}", expiration.before(now));

            return !expiration.before(now);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("토큰이 만료되었습니다: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("잘못된 형식의 토큰입니다: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("토큰 서명이 유효하지 않습니다: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("토큰 검증 실패: {}", e.getMessage(), e);
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public User findUserByToken(String token) {
        try {
            String userIdStr = extractUserId(token);
            log.info("Extracting user from token, userId string: {}", userIdStr);

            Integer userId = Integer.valueOf(userIdStr);
            log.info("Converted to Integer: {}", userId);

            User user = userJpaRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found in DB: userId={}", userId);
                        return new CustomRestfullException(
                                "유저 정보를 찾을 수 없습니다. (userId: " + userId + ")",
                                HttpStatus.NOT_FOUND
                        );
                    });

            log.info("User found: id={}, email={}, role={}",
                    user.getId(), user.getUserRole());

            return user;
        } catch (NumberFormatException e) {
            log.error("UserId 변환 실패: token subject is not a valid integer", e);
            throw new CustomRestfullException(
                    "토큰의 사용자 ID 형식이 올바르지 않습니다.",
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("findUserByToken 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}