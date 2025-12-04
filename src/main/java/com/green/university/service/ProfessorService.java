package com.green.university.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import com.green.university.dto.response.ReadSyllabusDto;
import com.green.university.dto.response.StudentInfoForProfessorDto;
import com.green.university.dto.response.SubjectForProfessorDto;
import com.green.university.dto.response.SubjectPeriodForProfessorDto;
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
    public List<Integer> selectSemester(Integer id) {
        List<Subject> subjects = subjectJpaRepository.findByProfessor_Id(id);

        // 학기만 추출 (중복 제거)
        List<Integer> semesters = subjects.stream()
                .map(Subject::getSemester)
                .distinct()
                .collect(Collectors.toList());

        return semesters;
    }

	// 년도와 학기, 교수 id를 이용하여 해당 과목의 정보 불러오기
    @Transactional(readOnly = true)
    public List<StuSub> selectBySubjectId(Integer subjectId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findBySubjectId(subjectId);
        return stuSubs;
    }

	// 과목 id로 과목 Entity 불러오기
	@Transactional
	public Subject selectSubjectById(Integer id) {
		Subject subjectEntity = subjectJpaRepository.findById(id).orElse(null);

		return subjectEntity;
	}

    /**
     *
     *
     * @param subjectPeriodForProfessorDto
     * @return SubjectForProfessorDto list
     */
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
    // 출결 및 성적 기입
    @Transactional
    public void updateGrade(UpdateStudentGradeDto updateStudentGradeDto) {

        // StuSubDetail 업데이트
        StuSubDetail stuSubDetail = stuSubDetailJpaRepository.findByStudentIdAndSubjectId(
                updateStudentGradeDto.getStudentId(),
                updateStudentGradeDto.getSubjectId()
        ).orElseThrow(() -> new CustomRestfullException("요청을 처리하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        stuSubDetail.setAbsent(updateStudentGradeDto.getAbsent());
        stuSubDetail.setLateness(updateStudentGradeDto.getLateness());
        stuSubDetail.setHomework(updateStudentGradeDto.getHomework());
        stuSubDetail.setMidExam(updateStudentGradeDto.getMidExam());
        stuSubDetail.setFinalExam(updateStudentGradeDto.getFinalExam());
        stuSubDetail.setConvertedMark(updateStudentGradeDto.getConvertedMark());

        stuSubDetailJpaRepository.save(stuSubDetail);

        // StuSub 업데이트
        StuSub stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(
                updateStudentGradeDto.getStudentId(),
                updateStudentGradeDto.getSubjectId()
        ).orElseThrow(() -> new CustomRestfullException("요청을 처리하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        stuSub.setGrade(updateStudentGradeDto.getGrade());

        stuSubJpaRepository.save(stuSub);
    }

	// 강의계획서 조회
    @Transactional(readOnly = true)
    public SyllaBus readSyllabus(Integer subjectId) {
        return syllaBusJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("강의계획서를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
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

        // 동적 쿼리 생성
        Specification<Professor> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 교수 ID로 검색
            if (form.getProfessorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("id"), form.getProfessorId()));
            }

            // 학과 ID로 검색
            if (form.getDeptId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("deptId"), form.getDeptId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return professorJpaRepository.findAll(spec, pageable);
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

}
