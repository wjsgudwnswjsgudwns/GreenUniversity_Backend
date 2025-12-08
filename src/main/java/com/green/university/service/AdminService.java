package com.green.university.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.green.university.repository.*;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.green.university.dto.CollTuitFormDto;
import com.green.university.dto.CollegeFormDto;
import com.green.university.dto.DepartmentFormDto;
import com.green.university.dto.RoomFormDto;
import com.green.university.dto.SubjectFormDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.utils.SubjectUtil;

/**
 * 
 * @author 박성희
 *
 */

@Service
public class AdminService {
    @Autowired
    private CollegeJpaRepository collegeJpaRepository;
    @Autowired
    private DepartmentJpaRepository departmentJpaRepository;
    @Autowired
    private CollTuitJpaRepository collTuitJpaRepository;
    @Autowired
    private RoomJpaRepository roomJpaRepository;
    // MyBatis 레포지토리: 강의 시간 중복 확인, 강의 등록/수정 등에 사용
    // JPA 레포지토리: 단순 조회/삭제에 사용
    @Autowired
    private SubjectJpaRepository subjectJpaRepository;
    @Autowired
    private SyllaBusJpaRepository syllaBusJpaRepository;
    @Autowired
    private ProfessorJpaRepository professorJpaRepository;


	// 단과대 입력 서비스
    @Transactional
    public void createCollege(@Validated CollegeFormDto collegeFormDto) {
        // 같은 이름 중복 검사(JPA)
        List<College> colleges = collegeJpaRepository.findAll();
        for (College c : colleges) {
            if (c.getName().equals(collegeFormDto.getName())) {
                throw new CustomRestfullException("이미 존재하는 단과대입니다", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        // 새 엔티티 생성 후 저장
        College college = new College();
        college.setName(collegeFormDto.getName());
        collegeJpaRepository.save(college);
    }

	// 단과대 조회 서비스
    @Transactional
    public List<College> readCollege() {
        // JPA를 사용하여 전체 단과대 목록 조회
        return collegeJpaRepository.findAllByOrderByIdAsc();
    }

	// 단과대 삭제 서비스
    public int deleteCollege(Integer id) {
        if (id == null) {
            return 0;
        }
        collegeJpaRepository.deleteById(id);
        return 1;
    }

	// 학과 입력 서비스
    @Transactional
    public void createDepartment(@Validated DepartmentFormDto departmentFormDto) {
        // 같은 학과 이름 중복 검사(JPA)
        List<Department> departments = departmentJpaRepository.findAll();
        for (Department dept : departments) {
            if (dept.getName().equals(departmentFormDto.getName())) {
                throw new CustomRestfullException("이미 존재하는 학과입니다", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        // 소속 단과대 조회
        College college = collegeJpaRepository.findById(departmentFormDto.getCollegeId())
                .orElseThrow(() -> new CustomRestfullException("해당 단과대가 존재하지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR));
        Department department = new Department();
        department.setName(departmentFormDto.getName());
        department.setCollege(college);
        departmentJpaRepository.save(department);
    }

	// 학과 조회 서비스
    public List<Department> readDepartment() {
        // JPA를 사용하여 전체 학과 목록 조회
        return departmentJpaRepository.findAllByOrderByIdAsc();
    }

	// 학과 삭제 서비스
    public int deleteDepartment(Integer collegeId) {
        if (collegeId == null) {
            return 0;
        }
        departmentJpaRepository.deleteById(collegeId);
        return 1;
    }

	// 학과 수정 서비스
    public int updateDepartment(DepartmentFormDto departmentFormDto) {
        if (departmentFormDto.getId() == null) {
            return 0;
        }
        return departmentJpaRepository.findById(departmentFormDto.getId()).map(dept -> {
            dept.setName(departmentFormDto.getName());
            // 소속 단과대 변경 시 처리
            if (departmentFormDto.getCollegeId() != null) {
                College college = collegeJpaRepository.findById(departmentFormDto.getCollegeId())
                        .orElse(null);
                if (college != null) {
                    dept.setCollege(college);
                }
            }
            departmentJpaRepository.save(dept);
            return 1;
        }).orElse(0);
    }

	// 단과대별 등록금 입력 서비스
    @Transactional
    public void createCollTuit(@Validated CollTuitFormDto collTuitFormDto) {
        // 등록금 중복 입력 검사(JPA)
        Integer collId = collTuitFormDto.getCollegeId();
        if (collId == null) {
            throw new CustomRestfullException("단과대 ID가 누락되었습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        boolean exists = collTuitJpaRepository.existsById(collId);
        if (exists) {
            throw new CustomRestfullException("이미 등록금이 입력된 학과입니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // 새 엔티티 생성 후 저장
        CollTuit collTuit = new CollTuit();
        collTuit.setCollegeId(collId);
        collTuit.setAmount(collTuitFormDto.getAmount());
        collTuitJpaRepository.save(collTuit);
    }

	// 단과대 등록금 조회 서비스
    public List<CollTuitFormDto> readCollTuit() {
        // JPA를 사용하여 등록금 목록 조회 후 DTO 변환
        List<CollTuit> collTuition = collTuitJpaRepository.findAllByOrderByCollegeIdAsc();
        List<CollTuitFormDto> dtos = new java.util.ArrayList<>();
        for (CollTuit ct : collTuition) {
            CollTuitFormDto dto = new CollTuitFormDto();
            dto.setCollegeId(ct.getCollegeId());
            dto.setAmount(ct.getAmount());
            // 단과대 이름은 CollegeJpaRepository에서 조회 가능
            College college = collegeJpaRepository.findById(ct.getCollegeId()).orElse(null);
            dto.setName(college != null ? college.getName() : null);
            dtos.add(dto);
        }
        return dtos;
    }

	// 단과대 등록금 삭제 서비스
    public int deleteCollTuit(Integer collegeId) {
        if (collegeId == null) {
            return 0;
        }
        collTuitJpaRepository.deleteById(collegeId);
        return 1;
    }

	// 단과대 등록금 수정 서비스
    public int updateCollTuit(CollTuitFormDto collTuitFormDto) {
        Integer collId = collTuitFormDto.getCollegeId();
        if (collId == null) {
            return 0;
        }
        return collTuitJpaRepository.findById(collId).map(ct -> {
            ct.setAmount(collTuitFormDto.getAmount());
            collTuitJpaRepository.save(ct);
            return 1;
        }).orElse(0);
    }

	// 강의실 입력 서비스
    @Transactional
    public void createRoom(@Validated RoomFormDto roomFormDto) {
        // 강의실 중복 입력 검사(JPA)
        if (roomFormDto.getId() == null) {
            throw new CustomRestfullException("강의실 ID가 누락되었습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        boolean exists = roomJpaRepository.existsById(roomFormDto.getId());
        if (exists) {
            throw new CustomRestfullException("이미 존재하는 강의실입니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // 소속 단과대 조회
        // RoomFormDto.collegeId는 문자열이므로 정수로 변환
        Integer collId;
        try {
            collId = Integer.valueOf(roomFormDto.getCollegeId());
        } catch (Exception e) {
            throw new CustomRestfullException("단과대 ID 형식이 올바르지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        College college = collegeJpaRepository.findById(collId)
                .orElseThrow(() -> new CustomRestfullException("해당 단과대가 존재하지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR));
        Room room = new Room();
        room.setId(roomFormDto.getId());
        room.setCollege(college);
        roomJpaRepository.save(room);
    }

	// 강의실 조회 서비스
    public List<Room> readRoom() {
        // JPA를 사용하여 전체 강의실 조회
        return roomJpaRepository.findAllByOrderByIdAsc();
    }

	// 강의실 삭제 서비스
    public int deleteRoom(String id) {
        if (id == null) {
            return 0;
        }
        roomJpaRepository.deleteById(id);
        return 1;
    }

	// 강의 입력 서비스
    @Transactional
    public List<Subject> createSubjectAndSyllabus(@Validated SubjectFormDto subjectFormDto) {
        // 강의실, 강의시간 중복 검사
        List<Subject> subjectList = subjectJpaRepository.findByRoom_IdAndSubDayAndSubYearAndSemester(
                subjectFormDto.getRoomId(),
                subjectFormDto.getSubDay(),
                subjectFormDto.getSubYear(),
                subjectFormDto.getSemester()
        );

        if (subjectList != null && !subjectList.isEmpty()) {
            SubjectUtil subjectUtil = new SubjectUtil();
            boolean result = subjectUtil.calculate(subjectFormDto, subjectList);
            if (result == false) {
                throw new CustomRestfullException("해당 시간대는 강의실을 사용중입니다! 다시 선택해주세요", HttpStatus.BAD_REQUEST);
            }
        }

        // DTO를 Entity로 변환
        Subject subject = new Subject();
        subject.setName(subjectFormDto.getName());

        // Professor 엔티티 조회 후 설정
        Professor professor = professorJpaRepository.findById(subjectFormDto.getProfessorId())
                .orElseThrow(() -> new CustomRestfullException("교수 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        subject.setProfessor(professor);

        // Room 엔티티 조회 후 설정
        Room room = roomJpaRepository.findById(subjectFormDto.getRoomId())
                .orElseThrow(() -> new CustomRestfullException("강의실 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        subject.setRoom(room);

        // Department 엔티티 조회 후 설정
        Department department = departmentJpaRepository.findById(subjectFormDto.getDeptId())
                .orElseThrow(() -> new CustomRestfullException("학과 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        subject.setDepartment(department);

        subject.setType(subjectFormDto.getType());
        subject.setSubYear(subjectFormDto.getSubYear());
        subject.setSemester(subjectFormDto.getSemester());
        subject.setSubDay(subjectFormDto.getSubDay());
        subject.setStartTime(subjectFormDto.getStartTime());
        subject.setEndTime(subjectFormDto.getEndTime());
        subject.setGrades(subjectFormDto.getGrades());
        subject.setCapacity(subjectFormDto.getCapacity());
        subject.setNumOfStudent(subjectFormDto.getNumOfStudent() != null ? subjectFormDto.getNumOfStudent() : 0);

        Subject savedSubject = subjectJpaRepository.save(subject);

        // 강의계획서에 강의 ID 저장
        SyllaBus syllaBus = new SyllaBus();
        syllaBus.setSubjectId(savedSubject.getId());
        syllaBusJpaRepository.save(syllaBus);

        return subjectList;
    }

	// 강의 조회 서비스
	public List<Subject> readSubject() {
        // JPA를 사용하여 전체 과목 목록을 조회한다.
        return subjectJpaRepository.findAllByOrderByIdAsc();
	}

    // 강의 검색 서비스 (수정 모드용 - 페이징 없음)
    public List<Subject> searchSubjectByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return subjectJpaRepository.findAllByOrderByIdAsc();
        }
        return subjectJpaRepository.findByNameContainingOrderByIdAsc(keyword);
    }

    // 페이징 & 검색
    public Map<String, Object> readSubjectWithPaging(String crud, int page, int size, String searchKeyword) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Subject> subjectPage;

        // 검색어가 있으면 검색, 없으면 전체 조회
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            subjectPage = subjectJpaRepository.findByNameContainingOrderByIdAsc(searchKeyword, pageable);
        } else {
            subjectPage = subjectJpaRepository.findAllByOrderByIdAsc(pageable);
        }

        List<College> collegeList = collegeJpaRepository.findAllByOrderByIdAsc();

        Map<String, Object> res = new HashMap<>();
        res.put("crud", crud);
        res.put("collegeList", collegeList.isEmpty() ? null : collegeList);
        res.put("subjectList", subjectPage.getContent());
        res.put("currentPage", subjectPage.getNumber());
        res.put("totalPages", subjectPage.getTotalPages());
        res.put("totalElements", subjectPage.getTotalElements());
        res.put("pageSize", subjectPage.getSize());

        return res;
    }

	// 강의 삭제 서비스
	public int deleteSubject(Integer id) {
        if (id == null) {
            return 0;
        }
        // 강의 계획서 레코드를 먼저 삭제한다.
        syllaBusJpaRepository.deleteById(id);
        // 과목 삭제
        subjectJpaRepository.deleteById(id);
        return 1;
	}

	// 강의 수정 서비스
    @Transactional
    public void updateSubject(SubjectFormDto subjectFormDto) {
        // ID로 기존 Subject 조회
        Subject subject = subjectJpaRepository.findById(subjectFormDto.getId())
                .orElseThrow(() -> new CustomRestfullException("강의 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        // roomId나 시간이 변경되는 경우에만 중복 검사
        boolean needsValidation = false;

        if (subjectFormDto.getRoomId() != null && !subjectFormDto.getRoomId().equals(subject.getRoom().getId())) {
            needsValidation = true;
        }
        if (subjectFormDto.getSubDay() != null && !subjectFormDto.getSubDay().equals(subject.getSubDay())) {
            needsValidation = true;
        }
        if (subjectFormDto.getStartTime() != null && !subjectFormDto.getStartTime().equals(subject.getStartTime())) {
            needsValidation = true;
        }
        if (subjectFormDto.getEndTime() != null && !subjectFormDto.getEndTime().equals(subject.getEndTime())) {
            needsValidation = true;
        }

        if (needsValidation) {
            // 중복 검사용 값 설정
            String checkRoomId = subjectFormDto.getRoomId() != null ? subjectFormDto.getRoomId() : subject.getRoom().getId();
            String checkSubDay = subjectFormDto.getSubDay() != null ? subjectFormDto.getSubDay() : subject.getSubDay();
            Integer checkStartTime = subjectFormDto.getStartTime() != null ? subjectFormDto.getStartTime() : subject.getStartTime();
            Integer checkEndTime = subjectFormDto.getEndTime() != null ? subjectFormDto.getEndTime() : subject.getEndTime();

            // 강의실, 강의시간 중복 검사
            List<Subject> subjectList = subjectJpaRepository.findByRoom_IdAndSubDayAndSubYearAndSemester(
                    checkRoomId,
                    checkSubDay,
                    subject.getSubYear(),
                    subject.getSemester()
            );

            if (subjectList != null && !subjectList.isEmpty()) {
                // 자기 자신은 제외
                subjectList = subjectList.stream()
                        .filter(s -> !s.getId().equals(subject.getId()))
                        .collect(java.util.stream.Collectors.toList());

                if (!subjectList.isEmpty()) {
                    SubjectFormDto checkDto = new SubjectFormDto();
                    checkDto.setId(subject.getId());
                    checkDto.setRoomId(checkRoomId);
                    checkDto.setSubDay(checkSubDay);
                    checkDto.setStartTime(checkStartTime);
                    checkDto.setEndTime(checkEndTime);
                    checkDto.setSubYear(subject.getSubYear());
                    checkDto.setSemester(subject.getSemester());

                    SubjectUtil subjectUtil = new SubjectUtil();
                    boolean result = subjectUtil.calculate(checkDto, subjectList);
                    if (result == false) {
                        throw new CustomRestfullException("해당 시간대는 강의실을 사용중입니다! 다시 선택해주세요", HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }

        // Entity 업데이트 (null이 아닌 값만)
        if (subjectFormDto.getName() != null && !subjectFormDto.getName().trim().isEmpty()) {
            subject.setName(subjectFormDto.getName());
        }

        // Professor 엔티티 조회 후 설정
        if (subjectFormDto.getProfessorId() != null) {
            Professor professor = professorJpaRepository.findById(subjectFormDto.getProfessorId())
                    .orElseThrow(() -> new CustomRestfullException("교수 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
            subject.setProfessor(professor);
        }

        // Room 엔티티 조회 후 설정
        if (subjectFormDto.getRoomId() != null && !subjectFormDto.getRoomId().trim().isEmpty()) {
            Room room = roomJpaRepository.findById(subjectFormDto.getRoomId())
                    .orElseThrow(() -> new CustomRestfullException("강의실 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
            subject.setRoom(room);
        }

        // Department 엔티티 조회 후 설정
        if (subjectFormDto.getDeptId() != null) {
            Department department = departmentJpaRepository.findById(subjectFormDto.getDeptId())
                    .orElseThrow(() -> new CustomRestfullException("학과 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
            subject.setDepartment(department);
        }

        if (subjectFormDto.getType() != null && !subjectFormDto.getType().trim().isEmpty()) {
            subject.setType(subjectFormDto.getType());
        }
        if (subjectFormDto.getSubDay() != null && !subjectFormDto.getSubDay().trim().isEmpty()) {
            subject.setSubDay(subjectFormDto.getSubDay());
        }
        if (subjectFormDto.getStartTime() != null) {
            subject.setStartTime(subjectFormDto.getStartTime());
        }
        if (subjectFormDto.getEndTime() != null) {
            subject.setEndTime(subjectFormDto.getEndTime());
        }
        if (subjectFormDto.getGrades() != null) {
            subject.setGrades(subjectFormDto.getGrades());
        }
        if (subjectFormDto.getCapacity() != null) {
            subject.setCapacity(subjectFormDto.getCapacity());
        }

        // save() 호출 (변경 감지로 자동 업데이트)
        subjectJpaRepository.save(subject);
    }


}
