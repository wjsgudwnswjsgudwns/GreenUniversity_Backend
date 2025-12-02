package com.green.university.service;

import java.util.List;

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
import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.ProfessorInfoDto;
import com.green.university.dto.response.StudentInfoDto;
import com.green.university.dto.response.StudentInfoStatListDto;
import com.green.university.dto.response.UserInfoForUpdateDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.interfaces.ProfessorRepository;
import com.green.university.repository.interfaces.StaffRepository;
import com.green.university.repository.interfaces.StuStatRepository;
import com.green.university.repository.interfaces.StudentRepository;
import com.green.university.repository.interfaces.UserRepository;
import com.green.university.repository.model.Staff;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.User;
import com.green.university.utils.Define;
import com.green.university.utils.TempPassword;

/**
 * 유저 서비스
 * 
 * @author 김지현
 */
@Service
public class UserService {

    // MyBatis 레포지토리
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;

    // JPA 레포지토리: 신규 생성 시 사용
    @Autowired
    private com.green.university.repository.StaffJpaRepository staffJpaRepository;
    @Autowired
    private com.green.university.repository.ProfessorJpaRepository professorJpaRepository;
    @Autowired
    private com.green.university.repository.StudentJpaRepository studentJpaRepository;
    @Autowired
    private com.green.university.repository.UserJpaRepository userJpaRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private StuStatService stuStatService;
	@Autowired
	private StuStatRepository stuStatRepository;

    // 추가 JPA 레포지토리: 학과 조회에 사용
    @Autowired
    private com.green.university.repository.DepartmentJpaRepository departmentJpaRepository;

	/**
	 * staff 생성 서비스로 먼저 staff_tb에 insert한 후 staff_tb에 생긴 id를 끌고와 user_tb에 생성함
	 * 
	 * @param createStaffDto
	 */
	@Transactional
	public void createStaffToStaffAndUser(CreateStaffDto createStaffDto) {
        // JPA를 사용하여 staff를 생성하고 user_tb에 계정을 추가한다.
        // Staff 엔티티 구성
        com.green.university.repository.model.Staff staff = new com.green.university.repository.model.Staff();
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
        com.green.university.repository.model.User user = new com.green.university.repository.model.User();
        user.setId(staffId);
        user.setPassword(passwordEncoder.encode(staffId + ""));
        user.setUserRole("staff");
        userJpaRepository.save(user);
	}

	/**
	 * professor 생성 서비스 먼저 professor_tb에 insert한 후 professor_tb에 생긴 id를 끌고와 user_tb에
	 * 생성함
	 * 
	 * @param createStaffDto
	 */
	@Transactional
	public void createProfessorToProfessorAndUser(CreateProfessorDto createProfessorDto) {
        // JPA를 사용하여 professor를 생성하고 user_tb에 계정을 추가한다.
        com.green.university.repository.model.Professor professor = new com.green.university.repository.model.Professor();
        professor.setName(createProfessorDto.getName());
        professor.setBirthDate(createProfessorDto.getBirthDate());
        professor.setGender(createProfessorDto.getGender());
        professor.setAddress(createProfessorDto.getAddress());
        professor.setTel(createProfessorDto.getTel());
        professor.setEmail(createProfessorDto.getEmail());
        // 교수는 학과에 소속되어야 하므로 deptId를 통해 Department 엔티티를 설정해야 한다.
        if (createProfessorDto.getDeptId() != null) {
            // Department JPA Repository를 통해 조회. 찾지 못하면 예외를 발생시키지 않고 null을 허용한다.
            com.green.university.repository.model.Department dept = null;
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
        com.green.university.repository.model.User user = new com.green.university.repository.model.User();
        user.setId(professorId);
        user.setPassword(passwordEncoder.encode(professorId + ""));
        user.setUserRole("professor");
        userJpaRepository.save(user);
	}

	/**
	 * professor 생성 서비스 먼저 professor_tb에 insert한 후 professor_tb에 생긴 id를 끌고와 user_tb에
	 * 생성함
	 * 
	 * @param createStaffDto
	 */
    @Transactional
    public void createStudentToStudentAndUser(CreateStudentDto createStudentDto) {
        // JPA를 사용하여 학생을 생성하고 user_tb에 계정을 추가한다.
        com.green.university.repository.model.Student student = new com.green.university.repository.model.Student();
        student.setName(createStudentDto.getName());
        student.setBirthDate(createStudentDto.getBirthDate());
        student.setGender(createStudentDto.getGender());
        student.setAddress(createStudentDto.getAddress());
        student.setTel(createStudentDto.getTel());
        student.setEmail(createStudentDto.getEmail());
        // 학과 설정
        if (createStudentDto.getDeptId() != null) {
            com.green.university.repository.model.Department dept = departmentJpaRepository.findById(createStudentDto.getDeptId()).orElse(null);
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

        // user 생성
        com.green.university.repository.model.User user = new com.green.university.repository.model.User();
        user.setId(studentId);
        user.setPassword(passwordEncoder.encode(studentId + ""));
        user.setUserRole("student");
        userJpaRepository.save(user);
    }

	@Transactional
	public PrincipalDto login(LoginDto loginDto) {
		PrincipalDto userEntity = userRepository.selectById(loginDto.getId());

		if (userEntity == null) {
			System.out.println("564156456");
			throw new CustomRestfullException(Define.NOT_FOUND_ID, HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (!passwordEncoder.matches(loginDto.getPassword(), userEntity.getPassword())) {
			throw new CustomRestfullException(Define.WRONG_PASSWORD, HttpStatus.BAD_REQUEST);
		}

		return userEntity;
	}

	/**
	 * 학생 수정 대상 정보 불러오기
	 * 
	 * @param userId
	 * @return 수정 대상 정보
	 */
	public UserInfoForUpdateDto readStudentInfoForUpdate(Integer userId) {

		UserInfoForUpdateDto userInfoForUpdateDto = studentRepository.selectByUserId(userId);

		return userInfoForUpdateDto;
	}

	/**
	 * 직원 수정 대상 정보 불러오기
	 * 
	 * @param userId
	 * @return 수정 대상 정보
	 */
	public UserInfoForUpdateDto readStaffInfoForUpdate(Integer userId) {

		UserInfoForUpdateDto userInfoForUpdateDto = staffRepository.selectByUserId(userId);

		return userInfoForUpdateDto;
	}

	/**
	 * 교수 수정 대상 정보 불러오기
	 * 
	 * @param userId
	 * @return 수정 대상 정보
	 */
	public UserInfoForUpdateDto readProfessorInfoForUpdate(Integer userId) {

		UserInfoForUpdateDto userInfoForUpdateDto = professorRepository.selectByUserId(userId);

		return userInfoForUpdateDto;
	}

	/**
	 * 학생 정보 수정
	 * 
	 * @param updateDto
	 */
	@Transactional
	public void updateStudent(UserUpdateDto updateDto) {

		int resultCountRaw = studentRepository.updateStudent(updateDto);
		if (resultCountRaw != 1) {
			throw new CustomRestfullException(Define.UPDATE_FAIL, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * 직원 정보 수정
	 * 
	 * @param updateDto
	 */
	@Transactional
	public void updateStaff(UserUpdateDto updateDto) {

		int resultCountRaw = staffRepository.updateStaff(updateDto);
		if (resultCountRaw != 1) {
			throw new CustomRestfullException(Define.UPDATE_FAIL, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * 교수 정보 수정
	 * 
	 * @param updateDto
	 */
	@Transactional
	public void updateProfessor(UserUpdateDto updateDto) {

		int resultCountRaw = professorRepository.updateProfessor(updateDto);
		if (resultCountRaw != 1) {
			throw new CustomRestfullException(Define.UPDATE_FAIL, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * 비밀번호 변경
	 * 
	 * @param changePasswordDto
	 */
	@Transactional
	public void updatePassword(ChangePasswordDto changePasswordDto) {
		int resultCountRaw = userRepository.updatePassword(changePasswordDto);
		if (resultCountRaw != 1) {
			throw new CustomRestfullException(Define.UPDATE_FAIL, HttpStatus.INTERNAL_SERVER_ERROR);
		}
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

	/**
	 * 교수 정보 조회
	 * 
	 * @param id
	 * @return professorEntity
	 */
	@Transactional
	public ProfessorInfoDto readProfessorInfo(Integer id) {
		ProfessorInfoDto professorEntity = professorRepository.selectProfessorInfoById(id);
		return professorEntity;
	}

	/**
	 * 학생 정보 조회
	 * 
	 * @param id
	 * @return StudentEntity
	 */
	@Transactional
	public StudentInfoDto readStudentInfo(Integer id) {
		StudentInfoDto studentEntity = studentRepository.selectStudentInfoById(id);
		return studentEntity;
	}

	/**
	 * 아이디 찾기
	 * 
	 * @param findIdFormDto
	 * @return
	 */
	@Transactional
	public Integer readIdByNameAndEmail(FindIdFormDto findIdFormDto) {

		Integer findId = null;
		if (findIdFormDto.getUserRole().equals("student")) {
			findId = studentRepository.selectIdByNameAndEmail(findIdFormDto);
		} else if (findIdFormDto.getUserRole().equals("professor")) {
			findId = professorRepository.selectIdByNameAndEmail(findIdFormDto);
		} else if (findIdFormDto.getUserRole().equals("staff")) {
			findId = staffRepository.selectIdByNameAndEmail(findIdFormDto);
		}

		if (findId == null) {
			throw new CustomRestfullException("아이디를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return findId;

	}

	/**
	 * 아이디 찾기
	 * 
	 * @param findIdFormDto
	 * @return
	 */
	@Transactional
	public String updateTempPassword(FindPasswordFormDto findPasswordFormDto) {

		String password = null;

		Integer findId = 0;

		if (findPasswordFormDto.getUserRole().equals("student")) {
			findId = studentRepository.selectStudentByIdAndNameAndEmail(findPasswordFormDto);
			if (findId == null) {
				throw new CustomRestfullException("조건에 맞는 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else if (findPasswordFormDto.getUserRole().equals("professor")) {
			findId = professorRepository.selectProfessorByIdAndNameAndEmail(findPasswordFormDto);
			if (findId == null) {
				throw new CustomRestfullException("조건에 맞는 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} else if (findPasswordFormDto.getUserRole().equals("staff")) {
			findId = staffRepository.selectStaffByIdAndNameAndEmail(findPasswordFormDto);
			if (findId == null) {
				throw new CustomRestfullException("조건에 맞는 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		password = new TempPassword().returnTempPassword();
		System.out.println(password);
		ChangePasswordDto changePasswordDto = new ChangePasswordDto();
		changePasswordDto.setAfterPassword(passwordEncoder.encode(password));
		changePasswordDto.setId(findPasswordFormDto.getId());
		userRepository.updatePassword(changePasswordDto);

		return password;

	}

	public List<StudentInfoStatListDto> readStudentInfoStatListByStudentId(Integer studentId) {

		List<StudentInfoStatListDto> list = stuStatRepository.selectStuStatListBystudentId(studentId);

		return list;
	}

}
