package com.green.university.service;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.green.university.dto.response.*;
import com.green.university.repository.*;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.ChangePasswordDto;
import com.green.university.dto.CreateProfessorDto;
import com.green.university.dto.CreateStaffDto;
import com.green.university.dto.CreateStudentDto;
import com.green.university.dto.FindIdFormDto;
import com.green.university.dto.FindPasswordFormDto;
import com.green.university.dto.LoginDto;
import com.green.university.dto.UserUpdateDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.utils.Define;
import com.green.university.utils.TempPassword;

/**
 * 유저 서비스
 *
 * @author 김지현
 */
@Service
public class UserService {


    // JPA 레포지토리: 신규 생성 시 사용
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private StaffJpaRepository staffJpaRepository;
    @Autowired
    private ProfessorJpaRepository professorJpaRepository;
    @Autowired
    private StudentJpaRepository studentJpaRepository;
    @Autowired
    private MeetingParticipantJpaRepository meetingParticipantJpaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StuStatService stuStatService;
    @Autowired
    private StuStatJpaRepository stuStatJpaRepository;

    // 추가 JPA 레포지토리: 학과 조회에 사용
    @Autowired
    private DepartmentJpaRepository departmentJpaRepository;

    @Autowired
    private AdvisorAssignmentService advisorAssignmentService;


    @Transactional
    public void createStaffToStaffAndUser(CreateStaffDto createStaffDto) {
        // JPA를 사용하여 staff를 생성하고 user_tb에 계정을 추가한다.
        // Staff 엔티티 구성
        Staff staff = new Staff();
        staff.setName(createStaffDto.getName());
        staff.setBirthDate(createStaffDto.getBirthDate());
        staff.setGender(createStaffDto.getGender());
        staff.setAddress(createStaffDto.getAddress());
        staff.setTel(createStaffDto.getTel());
        staff.setEmail(createStaffDto.getEmail());
        // hireDate는 입력 DTO에 없으므로 null로 둔다.
        // 저장 후 생성된 ID 반환
        staff = staffJpaRepository.save(staff);
        Integer staffId = staff.getId();
        // User 엔티티 생성
        User user = new User();
        user.setId(staffId);
        user.setPassword(passwordEncoder.encode(staffId + ""));
        user.setUserRole("staff");
        userJpaRepository.save(user);
    }


    @Transactional
    public void createProfessorToProfessorAndUser(CreateProfessorDto createProfessorDto) {
        // JPA를 사용하여 professor를 생성하고 user_tb에 계정을 추가한다.
        Professor professor = new Professor();
        professor.setName(createProfessorDto.getName());
        professor.setBirthDate(createProfessorDto.getBirthDate());
        professor.setGender(createProfessorDto.getGender());
        professor.setAddress(createProfessorDto.getAddress());
        professor.setTel(createProfessorDto.getTel());
        professor.setEmail(createProfessorDto.getEmail());
        // 교수는 학과에 소속되어야 하므로 deptId를 통해 Department 엔티티를 설정해야 한다.
        if (createProfessorDto.getDeptId() != null) {
            // Department JPA Repository를 통해 조회. 찾지 못하면 예외를 발생시키지 않고 null을 허용한다.
            Department dept = null;
            try {
                dept = departmentJpaRepository.findById(createProfessorDto.getDeptId()).orElse(null);
            } catch (Exception e) {
                dept = null;
            }
            professor.setDepartment(dept);
            // 복합키 매핑을 위해 deptId 필드 유지
            professor.setDeptId(createProfessorDto.getDeptId());
        }
        // hireDate는 입력 DTO에 없으므로 null로 둔다.
        professor = professorJpaRepository.save(professor);
        Integer professorId = professor.getId();
        // User 엔티티 생성
        User user = new User();
        user.setId(professorId);
        user.setPassword(passwordEncoder.encode(professorId + ""));
        user.setUserRole("professor");
        userJpaRepository.save(user);
    }


    @Transactional
    public void createStudentToStudentAndUser(CreateStudentDto createStudentDto) {
        System.out.println("DEBUG Student DTO = " + createStudentDto);
        // JPA를 사용하여 학생을 생성하고 user_tb에 계정을 추가한다.
        Student student = new Student();
        student.setName(createStudentDto.getName());
        student.setBirthDate(createStudentDto.getBirthDate());
        student.setGender(createStudentDto.getGender());
        student.setAddress(createStudentDto.getAddress());
        student.setTel(createStudentDto.getTel());
        student.setEmail(createStudentDto.getEmail());
        // 학과 설정
        if (createStudentDto.getDeptId() != null) {
            Department dept = departmentJpaRepository.findById(createStudentDto.getDeptId()).orElse(null);
            student.setDepartment(dept);
            student.setDeptId(createStudentDto.getDeptId());
        }
        // 기본 학년/학기 설정 (1학년 1학기)
        student.setGrade(1);
        student.setSemester(1);
        // 입학일 설정
        student.setEntranceDate(createStudentDto.getEntranceDate());
        // graduationDate는 null로 둔다.
        student = studentJpaRepository.save(student);
        Integer studentId = student.getId();

        // 학적 상태 생성 (재학)
        stuStatService.createFirstStatus(studentId);

        try {
            advisorAssignmentService.assignAdvisorToStudent(studentId);
        } catch (Exception e) {
            System.err.println("지도교수 배정 실패 (학생 생성은 완료): " + e.getMessage());
        }

        // user 생성
        User user = new User();
        user.setId(studentId);
        user.setPassword(passwordEncoder.encode(studentId + ""));
        user.setUserRole("student");
        userJpaRepository.save(user);
    }

    @Transactional
    public PrincipalDto login(LoginDto loginDto) {
        User user = userJpaRepository.findById(loginDto.getId())
                .orElseThrow(() -> new CustomRestfullException(
                        Define.NOT_FOUND_ID, HttpStatus.INTERNAL_SERVER_ERROR));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new CustomRestfullException(Define.WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        String name = null;
        String userRole = user.getUserRole();

        // 역할에 따라 이름 조회
        if ("student".equals(userRole)) {
            name = studentJpaRepository.findById(user.getId())
                    .map(Student::getName)
                    .orElse(null);
        } else if ("professor".equals(userRole)) {
            name = professorJpaRepository.findById(user.getId())
                    .map(Professor::getName)
                    .orElse(null);
        } else if ("staff".equals(userRole)) {
            name = staffJpaRepository.findById(user.getId())
                    .map(Staff::getName)
                    .orElse(null);
        }

        return PrincipalDto.builder()
                .id(user.getId())
                .password(user.getPassword())
                .userRole(userRole)
                .name(name)
                .build();
    }

    public UserInfoForUpdateDto readStudentInfoForUpdate(Integer userId) {
        Student student = studentJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomRestfullException(
                        "학생 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        UserInfoForUpdateDto dto = new UserInfoForUpdateDto();
        dto.setAddress(student.getAddress());
        dto.setTel(student.getTel());
        dto.setEmail(student.getEmail());
        return dto;
    }

    public UserInfoForUpdateDto readStaffInfoForUpdate(Integer userId) {
        Staff staff = staffJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomRestfullException(
                        "직원 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        UserInfoForUpdateDto dto = new UserInfoForUpdateDto();
        dto.setAddress(staff.getAddress());
        dto.setTel(staff.getTel());
        dto.setEmail(staff.getEmail());
        return dto;
    }

    public UserInfoForUpdateDto readProfessorInfoForUpdate(Integer userId) {
        Professor professor =
                professorJpaRepository.findById(userId)
                        .orElseThrow(() -> new CustomRestfullException(
                                "교수 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        UserInfoForUpdateDto dto = new UserInfoForUpdateDto();
        dto.setAddress(professor.getAddress());
        dto.setTel(professor.getTel());
        dto.setEmail(professor.getEmail());
        return dto;
    }

    @Transactional
    public void updateStudent(UserUpdateDto updateDto) {
        Student student = studentJpaRepository.findById(updateDto.getUserId())
                .orElseThrow(() -> new CustomRestfullException(
                        "학생 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        student.setAddress(updateDto.getAddress());
        student.setTel(updateDto.getTel());
        student.setEmail(updateDto.getEmail());

        studentJpaRepository.save(student);
    }

    @Transactional
    public void updateStaff(UserUpdateDto updateDto) {
        Staff staff = staffJpaRepository.findById(updateDto.getUserId())
                .orElseThrow(() -> new CustomRestfullException(
                        "직원 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        staff.setAddress(updateDto.getAddress());
        staff.setTel(updateDto.getTel());
        staff.setEmail(updateDto.getEmail());

        staffJpaRepository.save(staff);
    }

    @Transactional
    public void updateProfessor(UserUpdateDto updateDto) {
        Professor professor =
                professorJpaRepository.findById(updateDto.getUserId())
                        .orElseThrow(() -> new CustomRestfullException(
                                "교수 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        professor.setAddress(updateDto.getAddress());
        professor.setTel(updateDto.getTel());
        professor.setEmail(updateDto.getEmail());

        professorJpaRepository.save(professor);
    }

    /**
     * 비밀번호 변경
     *
     * @param changePasswordDto
     */
    @Transactional
    public void updatePassword(ChangePasswordDto changePasswordDto) {
        // ChangePasswordDto.afterPassword 는 이미 암호화된 상태로 들어옴
        User user = userJpaRepository.findById(changePasswordDto.getId())
                .orElseThrow(() -> new CustomRestfullException(
                        "사용자를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        user.setPassword(changePasswordDto.getAfterPassword());
        userJpaRepository.save(user);
    }

    /**
     * 학생 조회
     *
     * @param studentId
     * @return studentEntity
     */
    @Transactional
    public Student readStudent(Integer studentId) {
        // JPA를 사용하여 학생 엔티티 조회. 없을 경우 null 반환
        return studentJpaRepository.findById(studentId).orElse(null);
    }

    /**
     * 직원 조회
     *
     * @param id
     * @return staffEntity
     */
    @Transactional
    public Staff readStaff(Integer id) {
        // JPA를 사용하여 직원 엔티티 조회. 없을 경우 null 반환
        return staffJpaRepository.findById(id).orElse(null);
    }

    @Transactional
    public StudentInfoDto readStudentInfo(Integer id) {
        Student student = studentJpaRepository.findById(id)
                .orElseThrow(() -> new CustomRestfullException(
                        "학생 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        return new StudentInfoDto(student);
    }

    @Transactional
    public ProfessorInfoDto readProfessorInfo(Integer id) {
        Professor professor =
                professorJpaRepository.findById(id)
                        .orElseThrow(() -> new CustomRestfullException(
                                "교수 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        return new ProfessorInfoDto(professor);
    }


    @Transactional
    public Integer readIdByNameAndEmail(FindIdFormDto dto) {
        String role = dto.getUserRole();
        if (role == null) {
            throw new CustomRestfullException("userRole 값이 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        Integer findId = null;
        if ("student".equals(role)) {
            findId = studentJpaRepository
                    .findByNameAndEmail(dto.getName(), dto.getEmail())
                    .map(Student::getId)
                    .orElse(null);

        } else if ("professor".equals(role)) {
            findId = professorJpaRepository
                    .findByNameAndEmail(dto.getName(), dto.getEmail())
                    .map(Professor::getId)
                    .orElse(null);

        } else if ("staff".equals(role)) {
            findId = staffJpaRepository
                    .findByNameAndEmail(dto.getName(), dto.getEmail())
                    .map(Staff::getId)
                    .orElse(null);

        } else {
            throw new CustomRestfullException("지원하지 않는 userRole 입니다.", HttpStatus.BAD_REQUEST);
        }

        if (findId == null) {
            throw new CustomRestfullException("아이디를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return findId;

    }

    @Transactional
    public String updateTempPassword(FindPasswordFormDto dto) {

        String role = dto.getUserRole();
        if (role == null) {
            throw new CustomRestfullException("userRole 값이 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        Integer userId = null;

        if ("student".equals(role)) {
            userId = studentJpaRepository
                    .findByIdAndNameAndEmail(dto.getId(), dto.getName(), dto.getEmail())
                    .map(Student::getId)
                    .orElse(null);

        } else if ("professor".equals(role)) {
            userId = professorJpaRepository
                    .findByIdAndNameAndEmail(dto.getId(), dto.getName(), dto.getEmail())
                    .map(Professor::getId)
                    .orElse(null);

        } else if ("staff".equals(role)) {
            userId = staffJpaRepository
                    .findByIdAndNameAndEmail(dto.getId(), dto.getName(), dto.getEmail())
                    .map(Staff::getId)
                    .orElse(null);

        } else {
            throw new CustomRestfullException("지원하지 않는 userRole 입니다.", HttpStatus.BAD_REQUEST);
        }

        if (userId == null) {
            throw new CustomRestfullException("조건에 맞는 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String tempPw = new TempPassword().returnTempPassword();

        User user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomRestfullException(
                        "사용자 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        user.setPassword(passwordEncoder.encode(tempPw));
        userJpaRepository.save(user);

        return tempPw;
    }
    @Transactional(readOnly = true)
    public List<StudentInfoStatListDto> readStudentInfoStatListByStudentId(Integer studentId) {

        return stuStatJpaRepository
                .findByStudentIdOrderByIdDesc(studentId)
                .stream()
                .map(StudentInfoStatListDto::new)  // DTO 생성자 사용
                .collect(Collectors.toList());
    }


    @Transactional
    public void updateUserProfileWithPasswordCheck(
            Integer userId,
            String userRole,
            String rawPassword,
            UserInfoForUpdateDto dto
    ) {
        // 1) User 엔티티를 JPA로 조회
        User user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomRestfullException(
                        "사용자를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        // 2) 비밀번호 검증 (DB 기준)
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new CustomRestfullException(Define.WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        // 3) 역할에 따라 각 엔티티(Student/Staff/Professor) 업데이트
        if ("student".equals(userRole)) {
            updateStudentProfileWithJpa(userId, dto);
        } else if ("staff".equals(userRole)) {
            updateStaffProfileWithJpa(userId, dto);
        } else if ("professor".equals(userRole)) {
            updateProfessorProfileWithJpa(userId, dto);
        } else {
            throw new CustomRestfullException("지원하지 않는 사용자 유형입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    // 학생 정보 수정 (JPA)
    private void updateStudentProfileWithJpa(Integer userId, UserInfoForUpdateDto dto) {
        Student student =
                studentJpaRepository.findById(userId)
                        .orElseThrow(() -> new CustomRestfullException(
                                "학생 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        student.setAddress(dto.getAddress());
        student.setTel(dto.getTel());
        student.setEmail(dto.getEmail());

        studentJpaRepository.save(student);
    }

    // 직원 정보 수정 (JPA)
    private void updateStaffProfileWithJpa(Integer userId, UserInfoForUpdateDto dto) {
        Staff staff =
                staffJpaRepository.findById(userId)
                        .orElseThrow(() -> new CustomRestfullException(
                                "직원 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        staff.setAddress(dto.getAddress());
        staff.setTel(dto.getTel());
        staff.setEmail(dto.getEmail());

        staffJpaRepository.save(staff);
    }

    // 교수 정보 수정 (JPA)
    private void updateProfessorProfileWithJpa(Integer userId, UserInfoForUpdateDto dto) {
        Professor professor =
                professorJpaRepository.findById(userId)
                        .orElseThrow(() -> new CustomRestfullException(
                                "교수 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        professor.setAddress(dto.getAddress());
        professor.setTel(dto.getTel());
        professor.setEmail(dto.getEmail());

        professorJpaRepository.save(professor);
    }


    /**
     * 사용자 ID로 Principal 정보 조회
     * JWT 토큰에서 추출한 사용자 ID로 전체 정보를 가져올 때 사용
     */
    @Transactional(readOnly = true)
    public PrincipalDto readPrincipalById(Integer userId) {
        User user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomRestfullException(
                        "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        String name = null;
        String userRole = user.getUserRole();

        // 역할에 따라 이름 조회
        if ("student".equals(userRole)) {
            name = studentJpaRepository.findById(userId)
                    .map(Student::getName)
                    .orElse(null);
        } else if ("professor".equals(userRole)) {
            name = professorJpaRepository.findById(userId)
                    .map(Professor::getName)
                    .orElse(null);
        } else if ("staff".equals(userRole)) {
            name = staffJpaRepository.findById(userId)
                    .map(Staff::getName)
                    .orElse(null);
        }

        return PrincipalDto.builder()
                .id(user.getId())
                .password(user.getPassword())
                .userRole(userRole)
                .name(name)
                .build();
    }

    // UserService에 추가할 헬퍼 메서드들

    /**
     * User 엔티티를 PrincipalDto로 변환
     * 역할에 따라 이름을 조회하여 설정
     */
    public PrincipalDto convertToPrincipalDto(User user) {
        String name = getNameByUserRole(user.getId(), user.getUserRole());
        String email = getEmailByUser(user);

        return PrincipalDto.builder()
                .id(user.getId())
                .email(email)
                .password(user.getPassword())
                .userRole(user.getUserRole())
                .name(name)
                .build();
    }

    /**
     * 사용자 역할에 따라 이름 조회
     */
    private String getNameByUserRole(Integer userId, String userRole) {
        if ("student".equals(userRole)) {
            return studentJpaRepository.findById(userId)
                    .map(Student::getName)
                    .orElse(null);
        } else if ("professor".equals(userRole)) {
            return professorJpaRepository.findById(userId)
                    .map(Professor::getName)
                    .orElse(null);
        } else if ("staff".equals(userRole)) {
            return staffJpaRepository.findById(userId)
                    .map(Staff::getName)
                    .orElse(null);
        }
        return null;
    }
    /**
     * 유저 엔티티로 이메일 조회
     */
    private String getEmailByUser(User user) {
        Integer userId = user.getId();
        String userRole = user.getUserRole();

        if ("student".equals(userRole)) {
            return studentJpaRepository.findById(userId)
                    .map(Student::getEmail)
                    .orElse(null);
        } else if ("professor".equals(userRole)) {
            return professorJpaRepository.findById(userId)
                    .map(Professor::getEmail)
                    .orElse(null);
        } else if ("staff".equals(userRole)) {
            return staffJpaRepository.findById(userId)
                    .map(Staff::getEmail)
                    .orElse(null);
        }
        return null;
    }
    @Transactional(readOnly = true)
    public User readUserById(Integer userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomRestfullException(
                        "사용자 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
    }


    @Transactional(readOnly = true)
    public List<UserSearchItemResDto> searchUsersByRoleAndName(String role, String keyword, Integer meetingId) {
        if (role == null || role.trim().isEmpty()) {
            throw new CustomRestfullException("role 값이 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        String r = role.trim().toLowerCase();
        String q = (keyword == null) ? "" : keyword.trim();
        if (q.length() < 2) return List.of();

        // ✅ meetingId가 있으면, "이미 초대/참가 중"인 유저 id set 만들기
        final java.util.Set<Integer> alreadySet;
        if (meetingId == null) {
            alreadySet = java.util.Set.of();
        } else {
            var ids = meetingParticipantJpaRepository.findUserIdsByMeetingIdAndStatusIn(
                    meetingId,
                    java.util.List.of("INVITED", "JOINED")
            );
            alreadySet = new java.util.HashSet<>(ids);
        }

        switch (r) {
            case "student":
                return studentJpaRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q).stream()
                        .map(s -> new UserSearchItemResDto(
                                s.getId(), s.getName(), "student", s.getEmail(),
                                alreadySet.contains(s.getId())
                        ))
                        .toList();

            case "professor":
                return professorJpaRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q).stream()
                        .map(p -> new UserSearchItemResDto(
                                p.getId(), p.getName(), "professor", p.getEmail(),
                                alreadySet.contains(p.getId())
                        ))
                        .toList();

            case "staff":
                return staffJpaRepository.findByNameContainingIgnoreCaseOrderByNameAsc(q).stream()
                        .map(st -> new UserSearchItemResDto(
                                st.getId(), st.getName(), "staff", st.getEmail(),
                                alreadySet.contains(st.getId())
                        ))
                        .toList();

            default:
                throw new CustomRestfullException("지원하지 않는 role 입니다.", HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * UserService.java에 이 메서드를 추가하세요
     * 현재 사용자의 이메일 조회
     */
    @Transactional(readOnly = true)
    public String getCurrentEmail(Integer userId, String userRole) {
        if ("student".equals(userRole)) {
            return studentJpaRepository.findById(userId)
                    .map(Student::getEmail)
                    .orElseThrow(() -> new CustomRestfullException(
                            "학생 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
        } else if ("professor".equals(userRole)) {
            return professorJpaRepository.findById(userId)
                    .map(Professor::getEmail)
                    .orElseThrow(() -> new CustomRestfullException(
                            "교수 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
        } else if ("staff".equals(userRole)) {
            return staffJpaRepository.findById(userId)
                    .map(Staff::getEmail)
                    .orElseThrow(() -> new CustomRestfullException(
                            "직원 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
        } else {
            throw new CustomRestfullException("지원하지 않는 사용자 유형입니다.", HttpStatus.BAD_REQUEST);
        }
    }


}

