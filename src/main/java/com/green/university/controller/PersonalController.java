package com.green.university.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.green.university.jwt.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.green.university.dto.ChangePasswordDto;
import com.green.university.dto.FindIdFormDto;
import com.green.university.dto.FindPasswordFormDto;
import com.green.university.dto.LoginDto;
import com.green.university.dto.UserUpdateDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.ProfessorInfoDto;
import com.green.university.dto.response.StudentInfoDto;
import com.green.university.dto.response.StudentInfoStatListDto;
import com.green.university.dto.response.UserInfoForUpdateDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Staff;
import com.green.university.service.UserService;
import com.green.university.utils.Define;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PersonalController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 현재 인증된 사용자 정보 조회 헬퍼 메서드
     */
    private PrincipalDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CustomRestfullException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        Object principalObj = authentication.getPrincipal();

        if (principalObj instanceof PrincipalDto) {
            return (PrincipalDto) principalObj;
        }

        if (principalObj instanceof String username) {
            try {
                Integer userId = Integer.parseInt(username);
                return userService.readPrincipalById(userId);
            } catch (NumberFormatException e) {
                throw new CustomRestfullException("유효하지 않은 사용자 정보입니다.", HttpStatus.UNAUTHORIZED);
            }
        }

        throw new CustomRestfullException("유효하지 않은 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 로그인 - JWT 토큰 발급
     */
    @PostMapping("/auth/login")
    public ResponseEntity<?> signInProc(@Valid @RequestBody LoginDto loginDto) {

        // 1. 사용자 인증
        PrincipalDto principal = userService.login(loginDto);

        // 2. JWT 토큰 생성
        String token = jwtUtil.generateToken(
                principal.getId().toString(),
                principal.getUserRole()
        );

        // 3. 응답
        Map<String, Object> body = new HashMap<>();
        body.put("message", "로그인 성공");
        body.put("token", token);
        body.put("principal", principal);

        return ResponseEntity.ok(body);
    }

    /**
     * 로그아웃 (클라이언트에서 토큰 삭제)
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout() {
        // JWT는 stateless이므로 서버에서 할 일 없음
        // 클라이언트에서 토큰을 삭제하면 됨
        Map<String, String> body = new HashMap<>();
        body.put("message", "로그아웃 성공");
        return ResponseEntity.ok(body);
    }

    /**
     * 사용자 정보 수정
     */
    @PutMapping("/user/update")
    public ResponseEntity<?> updateUserProc(
            @Valid @RequestBody UserInfoForUpdateDto userInfoForUpdateDto,
            @RequestParam String password,
            @RequestParam(required = false) Boolean emailVerified) {

        PrincipalDto principal = getCurrentUser();

        // 이메일이 변경되었는지 확인
        String currentEmail = userService.getCurrentEmail(principal.getId(), principal.getUserRole());
        boolean emailChanged = !currentEmail.equals(userInfoForUpdateDto.getEmail());

        // 이메일이 변경된 경우 인증 확인 (emailVerified 파라미터로 전달받음)
        if (emailChanged) {
            if (emailVerified == null || !emailVerified) {
                throw new CustomRestfullException(
                        "이메일이 변경되었습니다. 이메일 인증을 완료해주세요.",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        userService.updateUserProfileWithPasswordCheck(
                principal.getId(),
                principal.getUserRole(),
                password,
                userInfoForUpdateDto
        );

        Map<String, String> body = new HashMap<>();
        body.put("message", "사용자 정보가 수정되었습니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/user/password")
    public ResponseEntity<?> updatePasswordProc(@Valid @RequestBody ChangePasswordDto changePasswordDto) {

        PrincipalDto principal = getCurrentUser();

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(changePasswordDto.getBeforePassword(), principal.getPassword())) {
            throw new CustomRestfullException(Define.WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        // 새 비밀번호 일치 확인
        if (!changePasswordDto.getAfterPassword().equals(changePasswordDto.getPasswordCheck())) {
            throw new CustomRestfullException("변경할 비밀번호와 비밀번호 확인은 같아야합니다.", HttpStatus.BAD_REQUEST);
        }

        changePasswordDto.setId(principal.getId());
        changePasswordDto.setAfterPassword(passwordEncoder.encode(changePasswordDto.getAfterPassword()));
        userService.updatePassword(changePasswordDto);

        Map<String, String> body = new HashMap<>();
        body.put("message", "비밀번호가 변경되었습니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * 학생 정보 조회
     */
    @GetMapping("/user/info/student")
    public ResponseEntity<?> readStudentInfo() {
        PrincipalDto principal = getCurrentUser();

        StudentInfoDto student = userService.readStudentInfo(principal.getId());
        List<StudentInfoStatListDto> list = userService.readStudentInfoStatListByStudentId(principal.getId());

        Map<String, Object> body = new HashMap<>();
        body.put("student", student);
        body.put("stustatList", list);
        return ResponseEntity.ok(body);
    }

    /**
     * 직원 정보 조회
     */
    @GetMapping("/user/info/staff")
    public ResponseEntity<?> readStaffInfo() {
        PrincipalDto principal = getCurrentUser();

        Staff staff = userService.readStaff(principal.getId());

        Map<String, Object> body = new HashMap<>();
        body.put("staff", staff);
        return ResponseEntity.ok(body);
    }

    /**
     * 교수 정보 조회
     */
    @GetMapping("/user/info/professor")
    public ResponseEntity<?> readProfessorInfo() {
        PrincipalDto principal = getCurrentUser();

        ProfessorInfoDto professor = userService.readProfessorInfo(principal.getId());

        Map<String, Object> body = new HashMap<>();
        body.put("professor", professor);
        return ResponseEntity.ok(body);
    }

    /**
     * 아이디 찾기 (인증 불필요)
     */
    @PostMapping("/auth/find/id")
    public ResponseEntity<?> findIdProc(@Valid @RequestBody FindIdFormDto findIdFormDto) {
        Integer findId = userService.readIdByNameAndEmail(findIdFormDto);

        Map<String, Object> body = new HashMap<>();
        body.put("id", findId);
        body.put("name", findIdFormDto.getName());
        return ResponseEntity.ok(body);
    }

    /**
     * 비밀번호 찾기 (인증 불필요)
     */
    @PostMapping("/auth/find/password")
    public ResponseEntity<?> findPasswordProc(@Valid @RequestBody FindPasswordFormDto findPasswordFormDto) {
        String password = userService.updateTempPassword(findPasswordFormDto);

        Map<String, Object> body = new HashMap<>();
        body.put("name", findPasswordFormDto.getName());
        body.put("password", password);
        return ResponseEntity.ok(body);
    }

    /**
     * 가이드 페이지
     */
    @GetMapping("/public/guide")
    public ResponseEntity<Map<String, String>> pop() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "비밀번호 관련 안내입니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * 현재 사용자 정보 조회 (토큰 검증용)
     */
    @GetMapping("/user/me")
    public ResponseEntity<?> getCurrentUserInfo() {
        PrincipalDto principal = getCurrentUser();

        Map<String, Object> body = new HashMap<>();
        body.put("principal", principal);
        return ResponseEntity.ok(body);
    }
}