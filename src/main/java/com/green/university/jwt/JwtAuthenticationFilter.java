package com.green.university.jwt;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.repository.model.User;
import com.green.university.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.info("=== JWT Filter Start ===");
        log.info("Request URI: {}", requestURI);
        log.info("Request Method: {}", request.getMethod());

        try {
            String token = parseJwt(request);
            log.info("Token extracted: {}", token != null ? "YES" : "NO");

            if (token != null) {
                log.info("Token (first 20 chars): {}", token.substring(0, Math.min(20, token.length())));

                boolean isValid = jwtUtil.validateToken(token);
                log.info("Token validation result: {}", isValid);

                boolean hasAuth = SecurityContextHolder.getContext().getAuthentication() != null;
                log.info("Already authenticated: {}", hasAuth);

                if (isValid && !hasAuth) {
                    try {
                        // 토큰에서 사용자 정보 추출
                        String userId = jwtUtil.extractUserId(token);
                        String role = jwtUtil.extractRole(token);
                        log.info("Extracted userId: {}, role: {}", userId, role);

                        // DB에서 사용자 조회
                        User user = jwtUtil.findUserByToken(token);
                        log.info("User found: id={}, email={}, role={}",
                                user.getId(), user.getUserRole());

                        if (user != null) {
                            // User → PrincipalDto
                            PrincipalDto principal = userService.convertToPrincipalDto(user);
                            log.info("PrincipalDto created: id={}, email={}",
                                    principal.getId(), principal.getEmail());

                            // 권한 생성
                            String userRole = user.getUserRole();
                            List<SimpleGrantedAuthority> authorities =
                                    userRole != null
                                            ? List.of(new SimpleGrantedAuthority("ROLE_" + userRole.toUpperCase()))
                                            : Collections.emptyList();
                            log.info("Authorities: {}", authorities);

                            // Authentication 설정
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            log.info("JWT 인증 성공: id={} email={} role={}",
                                    principal.getId(), principal.getEmail(), principal.getUserRole());
                        }
                    } catch (NumberFormatException e) {
                        log.error("UserId 변환 실패: {}", e.getMessage());
                    } catch (Exception e) {
                        log.error("User 조회 실패: {}", e.getMessage(), e);
                    }
                } else {
                    if (!isValid) {
                        log.warn("Token is invalid or expired");
                    }
                    if (hasAuth) {
                        log.info("User already authenticated, skipping");
                    }
                }
            } else {
                log.info("No token found in request");
            }
        } catch (Exception e) {
            log.error("JWT 인증 실패: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        log.info("=== JWT Filter End ===");
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        log.debug("Authorization Header: {}", headerAuth);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}