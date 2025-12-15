package com.green.university.service;

import java.util.ArrayList;
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

    // ✅ AI 분석 서비스 추가
    @Autowired
    private AIAnalysisResultService aiAnalysisResultService;

    // 교수가 맡은 과목들의 학기 검색
    @Transactional(readOnly = true)
    public List<SubjectPeriodForProfessorDto> selectSemester(Integer id) {
        List<Subject> subjects = subjectJpaRepository.findByProfessor_Id(id);

        List<SubjectPeriodForProfessorDto> periods = subjects.stream()
                .map(s -> new SubjectPeriodForProfessorDto(
                        null,
                        s.getSubYear(),
                        s.getSemester()
                ))
                .distinct()
                .sorted((a, b) -> {
                    int yearCompare = b.getSubYear().compareTo(a.getSubYear());
                    return yearCompare != 0 ? yearCompare : b.getSemester().compareTo(a.getSemester());
                })
                .collect(Collectors.toList());

        return periods;
    }

    public List<StuSubResponseDto> selectBySubjectId(Integer subjectId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findBySubjectId(subjectId);

        return stuSubs.stream().map(stuSub -> {
            Student st = stuSub.getStudent();
            StuSubDetail detail = stuSubDetailJpaRepository
                    .findByStudentIdAndSubjectId(st.getId(), subjectId)
                    .orElse(new StuSubDetail());

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

    @Transactional
    public Subject selectSubjectById(Integer id) {
        Subject subjectEntity = subjectJpaRepository.findById(id).orElse(null);
        return subjectEntity;
    }

    @Transactional(readOnly = true)
    public List<Subject> selectSubjectBySemester(SubjectPeriodForProfessorDto subjectPeriodForProfessorDto) {
        List<Subject> subjects = subjectJpaRepository.findByProfessor_IdAndSubYearAndSemester(
                subjectPeriodForProfessorDto.getId(),
                subjectPeriodForProfessorDto.getSubYear(),
                subjectPeriodForProfessorDto.getSemester()
        );
        return subjects;
    }

    // ✅ 출결 및 성적 기입 - AI 분석 트리거 추가
    @Transactional
    public void updateGrade(UpdateStudentGradeDto updateStudentGradeDto) {
        System.out.println("=== 성적 입력 시작 ===");

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

        // ✅ AI 분석 트리거 (실시간)
        triggerAIAnalysis(updateStudentGradeDto.getStudentId(), updateStudentGradeDto.getSubjectId());

        System.out.println("=== 성적 입력 완료 ===");
    }

    /**
     * ✅ AI 분석 트리거 (별도 메서드로 분리)
     */
    private void triggerAIAnalysis(Integer studentId, Integer subjectId) {
        try {
            System.out.println("🤖 AI 분석 시작: 학생 " + studentId + ", 과목 " + subjectId);

            StuSubDetail detail = stuSubDetailJpaRepository
                    .findByStudentIdAndSubjectId(studentId, subjectId)
                    .orElse(null);

            if (detail != null && detail.getSubject() != null) {
                aiAnalysisResultService.analyzeStudent(
                        studentId,
                        subjectId,
                        detail.getSubject().getSubYear(),
                        detail.getSubject().getSemester()
                );
                System.out.println("✅ AI 분석 완료");
            } else {
                System.out.println("⚠️ 과목 정보를 찾을 수 없어 AI 분석 생략");
            }

        } catch (Exception e) {
            System.err.println("⚠️ AI 분석 실패 (성적 입력은 정상 처리됨): " + e.getMessage());
            e.printStackTrace();
            // AI 분석 실패해도 성적 입력은 정상 유지
        }
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

        // 기본 과목 정보
        dto.setSubjectId(s.getId());
        dto.setSubjectName(s.getName());

        // 👇 교수 ID 추가
        dto.setProfessorId(p.getId());
        dto.setProfessorName(p.getName());

        // 강의 시간
        String classTime = String.format(
                "%s %02d:00 ~ %02d:00",
                s.getSubDay(),
                s.getStartTime(),
                s.getEndTime()
        );
        dto.setClassTime(classTime);

        // 강의실 및 학기 정보
        dto.setRoomId(s.getRoom().getId());
        dto.setSubYear(s.getSubYear());
        dto.setSemester(s.getSemester());
        dto.setGrades(s.getGrades());
        dto.setType(s.getType());

        // 학과 및 단과대 정보
        dto.setDeptName(d.getName());
        if (d.getCollege() != null) {
            dto.setCollegeName(d.getCollege().getName());
        }

        // 교수 연락처
        dto.setTel(p.getTel());
        dto.setEmail(p.getEmail());

        // 강의계획서 내용
        dto.setOverview(sb.getOverview());
        dto.setObjective(sb.getObjective());
        dto.setTextbook(sb.getTextbook());
        dto.setProgram(sb.getProgram());

        return dto;
    }

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

    @Transactional(readOnly = true)
    public Page<Professor> readProfessorList(ProfessorListForm form) {
        Pageable pageable = PageRequest.of(
                form.getPage(),
                20,
                Sort.by(Sort.Direction.ASC, "id")
        );

        if (form.getProfessorId() != null) {
            return professorJpaRepository.findByProfessorId(form.getProfessorId(), pageable);
        }

        if (form.getDeptId() != null) {
            return professorJpaRepository.findByDeptId(form.getDeptId(), pageable);
        }

        return professorJpaRepository.findAll(pageable);
    }

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

    public List<StuSubResponseDto> selectEnrolledStudentsBySubjectId(Integer subjectId) {
        System.out.println("=== 수강신청 학생 조회 시작 ===");
        System.out.println("과목 ID: " + subjectId);

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