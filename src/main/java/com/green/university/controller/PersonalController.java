package com.green.university.controller;

import java.util.List;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.
Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Map;
import java.util.HashMap;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.green.university.dto.ChangePasswordDto;
import com.green.university.dto.FindIdFormDto;
import com.green.university.dto.FindPasswordFormDto;
import com.green.university.dto.LoginDto;
import com.green.university.dto.NoticeFormDto;
import com.green.university.dto.UserUpdateDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.ProfessorInfoDto;
import com.green.university.dto.response.StudentInfoDto;
import com.green.university.dto.response.StudentInfoStatListDto;
import com.green.university.dto.response.UserInfoForUpdateDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.BreakApp;
import com.green.university.repository.model.Schedule;
import com.green.university.repository.model.Staff;
import com.green.university.repository.model.StuStat;
import com.green.university.service.BreakAppService;
import com.green.university.service.NoticeService;
import com.green.university.service.ScheuleService;
import com.green.university.service.StuStatService;
import com.green.university.service.UserService;
import com.green.university.utils.Define;

/**
 * User 로그인, 정보수정
 *
 * @author 김지현
 */
/**
 * 사용자 인증과 개인정보 관련 기능을 제공하는 REST 컨트롤러입니다. 로그인, 로그아웃,
 * 정보 수정, 비밀번호 변경 등 다양한 작업을 처리합니다. 기존에는 JSP 기반 뷰를
 * 반환했으나, 리액트 기반 클라이언트와의 통신을 위해 REST 스타일로 점진적으로
 * 전환하는 중입니다.
 */
@RestController
@RequestMapping("/api")
public class PersonalController {

    @Autowired
    private UserService userService;
    @Autowired
    private HttpSession session;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StuStatService stuStatService;
    @Autowired
    private BreakAppService breakAppService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ScheuleService scheuleService;

    /**
     * @author 서영 메인 홈페이지
     */
//    @GetMapping("")
//    public ResponseEntity<?> home() {
//        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
//        Map<String, Object> body = new HashMap<>();
//        // 공지사항 최신 글 5개
//        List<NoticeFormDto> noticeList = noticeService.readCurrentNotice();
//        body.put("noticeList", noticeList);
//        // 학사일정 (샘플: 2월)
//        List<Schedule> scheduleList = scheuleService.readScheduleListByMonth(2);
//        body.put("scheduleList", scheduleList);
//        if (principal.getUserRole().equals("student")) {
//            StudentInfoDto studentInfo = userService.readStudentInfo(principal.getId());
//            StuStat stuStat = stuStatService.readCurrentStatus(principal.getId());
//            body.put("userInfo", studentInfo);
//            body.put("currentStatus", stuStat.getStatus());
//        } else if (principal.getUserRole().equals("staff")) {
//            Staff staffInfo = userService.readStaff(principal.getId());
//            body.put("userInfo", staffInfo);
//            List<BreakApp> breakAppList = breakAppService.readByStatus("처리중");
//            body.put("breakAppSize", breakAppList.size());
//        } else {
//            ProfessorInfoDto professorInfo = userService.readProfessorInfo(principal.getId());
//            body.put("userInfo", professorInfo);
//        }
//        return ResponseEntity.ok(body);
//    }

    /**
     * 로그인 폼
     *
     * @return login.jsp
     */
//    @GetMapping("/login")
//    public ResponseEntity<Map<String, String>> login() {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "로그인 정보가 필요합니다.");
//        return ResponseEntity.ok(body);
//    }

    /*
     * 로그인 post 처리
     *
     * @param loginDto
     *
     * @return 메인 페이지 이동(수정 예정)
     */
    @PostMapping("/login")
    public ResponseEntity<?> signInProc(@Valid @RequestBody LoginDto loginDto, HttpServletResponse response,
                                        HttpServletRequest request, HttpSession session) {

        PrincipalDto principal = userService.login(loginDto);



        if ("on".equals(loginDto.getRememberId())) {
            Cookie cookie = new Cookie("id", principal.getId().toString());
            cookie.setMaxAge(60 * 60 * 24 * 7);
            cookie.setPath("/");
            response.addCookie(cookie);
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("id".equals(c.getName())) {
                        c.setMaxAge(0);
                        c.setPath("/");
                        response.addCookie(c);
                        break;
                    }
                }
            }
        }
        session.setAttribute(Define.PRINCIPAL, principal);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "로그인 성공");
        body.put("principal", principal);
        return ResponseEntity.ok(body);
    }

    /**
     * 개인정보 수정 페이지
     *
     * @param model
     * @return updateUser.jsp
     */
//    @GetMapping("/update")
//    public ResponseEntity<?> updateUser() {
//        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
//        UserInfoForUpdateDto userInfoForUpdateDto = null;
//        if ("staff".equals(principal.getUserRole())) {
//            userInfoForUpdateDto = userService.readStaffInfoForUpdate(principal.getId());
//        } else if ("student".equals(principal.getUserRole())) {
//            userInfoForUpdateDto = userService.readStudentInfoForUpdate(principal.getId());
//        } else if ("professor".equals(principal.getUserRole())) {
//            userInfoForUpdateDto = userService.readProfessorInfoForUpdate(principal.getId());
//        }
//        Map<String, Object> body = new HashMap<>();
//        body.put("userInfo", userInfoForUpdateDto);
//        return ResponseEntity.ok(body);
//    }

    /**
     * 개인정보 수정 페이지
     *
     * @param userInfoForUpdateDto, password
     * @return updateUser.jsp
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateUserProc(@Valid @RequestBody UserInfoForUpdateDto userInfoForUpdateDto,
                                            @RequestParam String password) {
        // 1. 세션에서 로그인 정보 가져오기
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        if (principal == null) {
            throw new CustomRestfullException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        // 2. 실제 업데이트는 모두 서비스에 위임
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


//    @GetMapping("/password")
//    public ResponseEntity<Map<String, String>> updatePassword() {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "비밀번호 변경 정보를 입력하세요.");
//        return ResponseEntity.ok(body);
//    }


    @PutMapping("/password")
    public ResponseEntity<?> updatePasswordProc(@Valid @RequestBody ChangePasswordDto changePasswordDto) {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        if (!passwordEncoder.matches(changePasswordDto.getBeforePassword(), principal.getPassword())) {
            throw new CustomRestfullException(Define.WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
        }
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
     * 로그아웃
     *
     * @return 로그인 페이지
     */
//    @GetMapping("/logout")
//    public ResponseEntity<Map<String, String>> logout() {
//        session.invalidate();
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "로그아웃되었습니다.");
//        return ResponseEntity.ok(body);
//    }


    @GetMapping("/info/student")
    public ResponseEntity<?> readStudentInfo() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        StudentInfoDto student = userService.readStudentInfo(principal.getId());
        List<StudentInfoStatListDto> list = userService.readStudentInfoStatListByStudentId(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("student", student);
        body.put("stustatList", list);
        return ResponseEntity.ok(body);
    }


    @GetMapping("/info/staff")
    public ResponseEntity<?> readStaffInfo() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Staff staff = userService.readStaff(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("staff", staff);
        return ResponseEntity.ok(body);
    }


    @GetMapping("/info/professor")
    public ResponseEntity<?> readProfessorInfo() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        ProfessorInfoDto professor = userService.readProfessorInfo(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("professor", professor);
        return ResponseEntity.ok(body);
    }

    /**
     * 아이디 찾기
     *
     * @return 아이디 찾기 페이지
    //	 */
//    @GetMapping("/find/id")
//    public ResponseEntity<Map<String, String>> findId() {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "아이디 찾기 요청을 보내세요.");
//        return ResponseEntity.ok(body);
//    }

    /**
     * 아이디 찾기 포스트
     *
     * @param findIdFormDto
     * @return 찾은 아이디 표시 페이지
     */
    @PostMapping("/find/id")
    public ResponseEntity<?> findIdProc(@Valid @RequestBody FindIdFormDto findIdFormDto) {
        Integer findId = userService.readIdByNameAndEmail(findIdFormDto);
        Map<String, Object> body = new HashMap<>();
        body.put("id", findId);
        body.put("name", findIdFormDto.getName());
        return ResponseEntity.ok(body);
    }

    /**
     * 비밀번호 찾기
     *
     * @return 아이디 찾기 페이지
     */
//    @GetMapping("/find/password")
//    public ResponseEntity<Map<String, String>> findPassword() {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "비밀번호 찾기 요청을 보내세요.");
//        return ResponseEntity.ok(body);
//    }

    @PostMapping("/find/password")
    public ResponseEntity<?> findPasswordProc(@Valid @RequestBody FindPasswordFormDto findPasswordFormDto) {
        String password = userService.updateTempPassword(findPasswordFormDto);
        Map<String, Object> body = new HashMap<>();
        body.put("name", findPasswordFormDto.getName());
        body.put("password", password);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/guide")
    public ResponseEntity<Map<String, String>> pop() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "비밀번호 관련 안내입니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * @return 에러페이지
     */
//    @GetMapping("/error")
//    public ResponseEntity<Map<String, String>> handleError() {
//        Map<String, String> body = new HashMap<>();
//        body.put("message", "에러가 발생했습니다.");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
//    }
}
