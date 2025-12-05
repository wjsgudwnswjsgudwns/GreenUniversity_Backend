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

        try {
            String token = parseJwt(request);

            if (token != null
                    && jwtUtil.validateToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                //String username = jwtUtil.extractUsername(token);
                User user = jwtUtil.findUserByToken(token);
                if (user != null) {
                    // 2) User → PrincipalDto
                    PrincipalDto principal = userService.convertToPrincipalDto(user);

                    // 3) 권한 생성 (프로젝트 규칙에 맞춰서 ROLE_ prefix 여부 조정)
                    String role = user.getUserRole();
                    List<SimpleGrantedAuthority> authorities =
                            role != null
                                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                                    : Collections.emptyList();

                    // 4) principal 에 PrincipalDto 세팅
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT 인증 성공: id={} email={} role={}",
                            principal.getId(), principal.getEmail(), principal.getUserRole());
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }


}