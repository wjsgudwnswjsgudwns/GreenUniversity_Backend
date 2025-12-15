package com.green.university.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.green.university.dto.response.*;
import com.green.university.repository.*;
import com.green.university.repository.model.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.ProfessorListForm;
import com.green.university.dto.SyllaBusFormDto;
import com.green.university.dto.UpdateStudentGradeDto;
import com.green.university.handler.exception.CustomRestfullException;


/**
 * 
 * @author 김지현
 */
@Service
public class ProfessorService {

    @Autowired
    private SubjectJpaRepository subjectJpaRepository;
    @Autowired
    private StuSubJpaRepository stuSubJpaRepository;
    @Autowired
    private StuSubDetailJpaRepository stuSubDetailJpaRepository;
    @Autowired
    private SyllaBusJpaRepository syllaBusJpaRepository;
    @Autowired
    private ProfessorJpaRepository professorJpaRepository;

    // 교수가 맡은 과목들의 학기 검색
    @Transactional(readOnly = true)
    public List<SubjectPeriodForProfessorDto> selectSemester(Integer id) {
        List<Subject> subjects = subjectJpaRepository.findByProfessor_Id(id);

        // 년도와 학기를 함께 반환 (중복 제거)
        // id는 null로 설정 (년도와 학기만 필요)
        List<SubjectPeriodForProfessorDto> periods = subjects.stream()
                .map(s -> new SubjectPeriodForProfessorDto(
                        null,  // id는 필요 없음
                        s.getSubYear(),
                        s.getSemester()
                ))
                .distinct()
                .sorted((a, b) -> {
                    // 년도 내림차순, 같으면 학기 내림차순
                    int yearCompare = b.getSubYear().compareTo(a.getSubYear());
                    return yearCompare != 0 ? yearCompare : b.getSemester().compareTo(a.getSemester());
                })
                .collect(Collectors.toList());

        return periods;
    }

	// 년도와 학기, 교수 id를 이용하여 해당 과목의 정보 불러오기
    public List<StuSubResponseDto> selectBySubjectId(Integer subjectId) {

        List<StuSub> stuSubs = stuSubJpaRepository.findBySubjectId(subjectId);

        return stuSubs.stream().map(stuSub -> {

            Student st = stuSub.getStudent();

            // 🔥 StuSubDetail 가져오기
            StuSubDetail detail = stuSubDetailJpaRepository
                    .findByStudentIdAndSubjectId(st.getId(), subjectId)
                    .orElse(new StuSubDetail()); // null 방지

            return new StuSubResponseDto(
                    st.getId(),
                    st.getName(),
                    st.getDepartment().getName(),
                    detail.getAbsent(),
                    detail.getLateness(),
                    detail.getHomework(),
                    detail.getMidExam(),
                    detail.getFinalExam(),
                    detail.getConvertedMark()
            );

        }).toList();
    }

	// 과목 id로 과목 Entity 불러오기
	@Transactional
	public Subject selectSubjectById(Integer id) {
		Subject subjectEntity = subjectJpaRepository.findById(id).orElse(null);

		return subjectEntity;
	}

    //
    @Transactional(readOnly = true)
    public List<Subject> selectSubjectBySemester(SubjectPeriodForProfessorDto subjectPeriodForProfessorDto) {
        List<Subject> subjects = subjectJpaRepository.findByProfessor_IdAndSubYearAndSemester(
                subjectPeriodForProfessorDto.getId(),
                subjectPeriodForProfessorDto.getSubYear(),
                subjectPeriodForProfessorDto.getSemester()
        );
        return subjects;
    }

	// 출결 및 성적 기입
    @Transactional
    public void updateGrade(UpdateStudentGradeDto updateStudentGradeDto) {

        // StuSubDetail 업데이트
        StuSubDetail stuSubDetail = stuSubDetailJpaRepository.findByStudentIdAndSubjectId(
                updateStudentGradeDto.getStudentId(),
                updateStudentGradeDto.getSubjectId()
        ).orElseThrow(() -> {
            System.out.println("❌ StuSubDetail을 찾을 수 없음!");
            return new CustomRestfullException("StuSubDetail을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        });

        System.out.println("✅ StuSubDetail 찾음: " + stuSubDetail.getId());

        stuSubDetail.setAbsent(updateStudentGradeDto.getAbsent());
        stuSubDetail.setLateness(updateStudentGradeDto.getLateness());
        stuSubDetail.setHomework(updateStudentGradeDto.getHomework());
        stuSubDetail.setMidExam(updateStudentGradeDto.getMidExam());
        stuSubDetail.setFinalExam(updateStudentGradeDto.getFinalExam());
        stuSubDetail.setConvertedMark(updateStudentGradeDto.getConvertedMark());

        stuSubDetailJpaRepository.save(stuSubDetail);
        System.out.println("✅ StuSubDetail 저장 완료");

        // StuSub 업데이트
        StuSub stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(
                updateStudentGradeDto.getStudentId(),
                updateStudentGradeDto.getSubjectId()
        ).orElseThrow(() -> {
            System.out.println("❌ StuSub을 찾을 수 없음!");
            return new CustomRestfullException("StuSub을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        });

        System.out.println("✅ StuSub 찾음");

        stuSub.setGrade(updateStudentGradeDto.getGrade());

        stuSubJpaRepository.save(stuSub);
        System.out.println("✅ StuSub 저장 완료");
        System.out.println("=== updateGrade 종료 ===");
    }

    // 강의계획서 조회
    @Transactional(readOnly = true)
    public SyllabusResponseDto readSyllabus(Integer subjectId) {

        SyllaBus sb = syllaBusJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("강의계획서를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        Subject s = sb.getSubject();
        Professor p = s.getProfessor();
        Department d = p.getDepartment();

        SyllabusResponseDto dto = new SyllabusResponseDto();

        // 기본 정보
        dto.setSubjectId(s.getId());
        dto.setSubjectName(s.getName());
        dto.setProfessorName(p.getName());

        // 수업 시간 포맷
        String classTime = String.format(
                "%s %02d:00 ~ %02d:00",
                s.getSubDay(),
                s.getStartTime(),
                s.getEndTime()
        );
        dto.setClassTime(classTime);

        dto.setRoomId(s.getRoom().getId());

        // 학사 정보
        dto.setSubYear(s.getSubYear());
        dto.setSemester(s.getSemester());
        dto.setGrades(s.getGrades());
        dto.setType(s.getType());

        // 교수 정보
        dto.setDeptName(d.getName());
        dto.setTel(p.getTel());
        dto.setEmail(p.getEmail());

        // 강의계획서 상세
        dto.setOverview(sb.getOverview());
        dto.setObjective(sb.getObjective());
        dto.setTextbook(sb.getTextbook());
        dto.setProgram(sb.getProgram());

        return dto;
    }

	/**
	 * 강의 계획서 업데이트
	 * 
	 * @param
	 */
    @Transactional
    public void updateSyllabus(SyllaBusFormDto syllaBusFormDto) {

        SyllaBus syllaBus = syllaBusJpaRepository.findById(syllaBusFormDto.getSubjectId())
                .orElseThrow(() -> new CustomRestfullException("제출 실패", HttpStatus.INTERNAL_SERVER_ERROR));

        syllaBus.setOverview(syllaBusFormDto.getOverview());
        syllaBus.setObjective(syllaBusFormDto.getObjective());
        syllaBus.setTextbook(syllaBusFormDto.getTextbook());
        syllaBus.setProgram(syllaBusFormDto.getProgram());

        syllaBusJpaRepository.save(syllaBus);
    }

	/**
	 * @return 교수 리스트 조회
	 */
    @Transactional(readOnly = true)
    public Page<Professor> readProfessorList(ProfessorListForm form) {
        // 페이지 번호는 0-based, 한 페이지당 20개
        Pageable pageable = PageRequest.of(
                form.getPage(),
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        // 교수 ID로 검색
        if (form.getProfessorId() != null) {
            return professorJpaRepository.findByProfessorId(form.getProfessorId(), pageable);
        }

        // 학과 ID로 검색
        if (form.getDeptId() != null) {
            return professorJpaRepository.findByDeptId(form.getDeptId(), pageable);
        }

        // 조건 없으면 전체 조회
        return professorJpaRepository.findAll(pageable);
    }

	/**
	 * 
	 * @param
	 * @return 교수 수
	 */
    @Transactional(readOnly = true)
    public Integer readProfessorAmount(ProfessorListForm professorListForm) {

        Integer amount = null;

        if (professorListForm.getDeptId() != null) {
            amount = (int) professorJpaRepository.countByDepartment_Id(professorListForm.getDeptId());
        } else {
            amount = (int) professorJpaRepository.count();
        }

        return amount;
    }

    /**
     * 특정 과목의 실제 수강신청한 학생 목록 조회
     * stu_sub_tb 기준 (예비 수강신청 제외)
     *
     * @param subjectId 과목 ID
     * @return 수강신청한 학생 DTO 리스트
     */
    public List<StuSubResponseDto> selectEnrolledStudentsBySubjectId(Integer subjectId) {
        System.out.println("=== 수강신청 학생 조회 시작 ===");
        System.out.println("과목 ID: " + subjectId);

        // ✅ 수정: findEnrolledBySubjectId 사용
        List<StuSub> enrollments = stuSubJpaRepository.findEnrolledBySubjectId(subjectId);

        System.out.println("조회된 학생 수: " + enrollments.size());

        for (StuSub enrollment : enrollments) {
            System.out.println("Student ID: " + enrollment.getStudentId() +
                    ", Enrollment Type: " + enrollment.getEnrollmentType());
        }

        List<StuSubResponseDto> studentList = new ArrayList<>();

        for (StuSub enrollment : enrollments) {
            if (enrollment.getStudent() != null) {
                Student student = enrollment.getStudent();

                StuSubResponseDto dto = new StuSubResponseDto();
                dto.setStudentId(student.getId());
                dto.setStudentName(student.getName());

                if (student.getDepartment() != null) {
                    dto.setDeptName(student.getDepartment().getName());
                } else {
                    dto.setDeptName("-");
                }

                studentList.add(dto);
            }
        }

        return studentList;
    }

}
