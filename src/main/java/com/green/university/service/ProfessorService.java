package com.green.university.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.green.university.dto.response.*;
import com.green.university.repository.*;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.GradingPolicyDto;
import com.green.university.dto.ProfessorListForm;
import com.green.university.dto.SyllaBusFormDto;
import com.green.university.dto.UpdateStudentGradeDto;
import com.green.university.handler.exception.CustomRestfullException;

import java.util.concurrent.ConcurrentHashMap;

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
    
    @Autowired
    private GradeJpaRepository gradeJpaRepository;

    // 과목별 가중치 정책 저장 (메모리 캐시, 향후 DB로 이전 가능)
    private final ConcurrentHashMap<Integer, GradingPolicyDto> gradingPolicyCache = new ConcurrentHashMap<>();

//    // AI 분석 서비스
//    @Autowired
//    private AIAnalysisResultService aiAnalysisResultService;

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
        GradingPolicyDto policy = getGradingPolicy(subjectId);

        // 1단계: 모든 학생 데이터 생성 및 환산 점수 계산
        List<StuSubResponseDto> allStudents = new ArrayList<>();
        
        for (StuSub stuSub : stuSubs) {
            Student st = stuSub.getStudent();
            StuSubDetail detail = stuSubDetailJpaRepository
                    .findByStudentIdAndSubjectId(st.getId(), subjectId)
                    .orElse(new StuSubDetail());

            StuSubResponseDto dto = new StuSubResponseDto();
            dto.setStudentId(st.getId());
            dto.setStudentName(st.getName());
            dto.setDeptName(st.getDepartment().getName());
            dto.setAbsent(detail.getAbsent() != null ? detail.getAbsent() : 0);
            dto.setLateness(detail.getLateness() != null ? detail.getLateness() : 0);
            dto.setHomework(detail.getHomework() != null ? detail.getHomework() : 0);
            dto.setMidExam(detail.getMidExam() != null ? detail.getMidExam() : 0);
            dto.setFinalExam(detail.getFinalExam() != null ? detail.getFinalExam() : 0);
            dto.setConvertedMark(detail.getConvertedMark());
            dto.setCurrentGrade(stuSub.getGrade());
            
            // 환산 점수 계산
            Double computedMark = calculateConvertedMark(detail, policy);
            dto.setComputedMark(computedMark);
            
            allStudents.add(dto);
        }

        // 2단계: 결석 4회 이상인 학생을 F 그룹으로 분류
        for (StuSubResponseDto student : allStudents) {
            if (student.getAbsent() >= 4) {
                student.setGroup("F");
                student.setRecommendedGrade("F");
            }
        }

        // 3단계: F 그룹이 아닌 학생들을 computedMark 기준으로 정렬하여 그룹 분류
        List<StuSubResponseDto> nonFStudents = allStudents.stream()
                .filter(s -> !"F".equals(s.getGroup()))
                .filter(s -> s.getComputedMark() != null)
                .sorted((a, b) -> Double.compare(
                        b.getComputedMark() != null ? b.getComputedMark() : 0.0,
                        a.getComputedMark() != null ? a.getComputedMark() : 0.0))
                .collect(Collectors.toList());

        int totalCount = nonFStudents.size();
        if (totalCount > 0) {
            int top30Count = (int) Math.ceil(totalCount * 0.3);
            int middle40Count = (int) Math.ceil(totalCount * 0.4);
            
            for (int i = 0; i < nonFStudents.size(); i++) {
                StuSubResponseDto student = nonFStudents.get(i);
                if (i < top30Count) {
                    // 상위 30%
                    student.setGroup("A");
                    student.setRecommendedGrade("A");
                } else if (i < top30Count + middle40Count) {
                    // 중간 40%
                    student.setGroup("B");
                    student.setRecommendedGrade("B");
                } else {
                    // 하위 30%
                    student.setGroup("C");
                    student.setRecommendedGrade("C");
                }
            }
        }

        // 4단계: computedMark가 null인 학생들은 C 그룹으로 분류
        for (StuSubResponseDto student : allStudents) {
            if (student.getGroup() == null) {
                student.setGroup("C");
                student.setRecommendedGrade("C");
            }
        }
        
        // 5단계: 최종 결과를 computedMark 기준 내림차순 정렬
        allStudents.sort((a, b) -> {
            // F 그룹은 항상 맨 아래로
            boolean aIsF = "F".equals(a.getGroup());
            boolean bIsF = "F".equals(b.getGroup());
            if (aIsF && !bIsF) return 1;
            if (!aIsF && bIsF) return -1;
            
            // F 그룹끼리는 결석 횟수 내림차순 (결석이 많은 순)
            if (aIsF && bIsF) {
                return Integer.compare(b.getAbsent() != null ? b.getAbsent() : 0,
                                     a.getAbsent() != null ? a.getAbsent() : 0);
            }
            
            // 나머지는 computedMark 기준 내림차순
            Double aMark = a.getComputedMark() != null ? a.getComputedMark() : 0.0;
            Double bMark = b.getComputedMark() != null ? b.getComputedMark() : 0.0;
            return Double.compare(bMark, aMark);
        });
        
        return allStudents;
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

        // Grade 값 검증 및 변환 (DB에 존재하는 형식으로 변환)
        String gradeValue = validateAndConvertGrade(updateStudentGradeDto.getGrade());
        stuSub.setGrade(gradeValue);
        stuSubJpaRepository.save(stuSub);
        System.out.println("✅ StuSub 저장 완료 - Grade: " + gradeValue);

        // ✅ AI 분석 트리거 (실시간)
//        triggerAIAnalysis(updateStudentGradeDto.getStudentId(), updateStudentGradeDto.getSubjectId());

        System.out.println("=== 성적 입력 완료 ===");
    }

    /**
     * ✅ AI 분석 트리거 (별도 메서드로 분리)
     */
//    private void triggerAIAnalysis(Integer studentId, Integer subjectId) {
//        try {
//            System.out.println("🤖 AI 분석 시작: 학생 " + studentId + ", 과목 " + subjectId);
//
//            StuSubDetail detail = stuSubDetailJpaRepository
//                    .findByStudentIdAndSubjectId(studentId, subjectId)
//                    .orElse(null);
//
//            if (detail != null && detail.getSubject() != null) {
//                aiAnalysisResultService.analyzeStudent(
//                        studentId,
//                        subjectId,
//                        detail.getSubject().getSubYear(),
//                        detail.getSubject().getSemester()
//                );
//                System.out.println("✅ AI 분석 완료");
//            } else {
//                System.out.println("⚠️ 과목 정보를 찾을 수 없어 AI 분석 생략");
//            }
//
//        } catch (Exception e) {
//            System.err.println("⚠️ AI 분석 실패 (성적 입력은 정상 처리됨): " + e.getMessage());
//            e.printStackTrace();
//            // AI 분석 실패해도 성적 입력은 정상 유지
//        }
//    }

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

    /**
     * 과목별 가중치 정책 조회 (없으면 기본값 반환)
     */
    public GradingPolicyDto getGradingPolicy(Integer subjectId) {
        return gradingPolicyCache.getOrDefault(subjectId, new GradingPolicyDto());
    }

    /**
     * 과목별 가중치 정책 저장
     */
    public void saveGradingPolicy(Integer subjectId, GradingPolicyDto policy) {
        // 가중치 합계 검증
        int sum = policy.getAttendanceWeight() + policy.getHomeworkWeight() 
                + policy.getMidtermWeight() + policy.getFinalWeight();
        if (sum != 100) {
            throw new CustomRestfullException("가중치 합계가 100이어야 합니다.", HttpStatus.BAD_REQUEST);
        }
        
        gradingPolicyCache.put(subjectId, policy);
    }

    /**
     * 환산 점수 계산
     * 출결, 과제, 중간고사, 기말고사 점수를 가중치에 따라 계산
     */
    private Double calculateConvertedMark(StuSubDetail detail, GradingPolicyDto policy) {
        if (detail == null || policy == null) {
            return null;
        }

        // 출결 점수 계산
        Integer absent = detail.getAbsent() != null ? detail.getAbsent() : 0;
        Integer lateness = detail.getLateness() != null ? detail.getLateness() : 0;
        
        // 지각을 결석으로 환산 (초과분만)
        int latenessAsAbsent = 0;
        if (lateness > policy.getLatenessFreeCount()) {
            int excessLateness = lateness - policy.getLatenessFreeCount();
            latenessAsAbsent = excessLateness / policy.getLatenessPerAbsent();
        }
        int totalAbsent = absent + latenessAsAbsent;
        
        // 출결 점수: 출결 만점에서 결석/지각으로 인한 감점 계산
        // 총 수업일수는 15주 기준으로 가정 (실제로는 Subject나 별도 설정 필요)
        int totalClasses = 15;
        double attendanceScore = Math.max(0, 
            policy.getAttendanceMax() - (totalAbsent * policy.getAttendanceMax() / totalClasses));
        
        // 출결 점수에 지각 감점 추가 적용
        if (lateness > policy.getLatenessFreeCount()) {
            int excessLateness = lateness - policy.getLatenessFreeCount();
            attendanceScore = Math.max(0, attendanceScore - (excessLateness * policy.getLatenessPenaltyPer()));
        }
        
        double attendancePart = (attendanceScore / policy.getAttendanceMax()) * policy.getAttendanceWeight();

        // 과제 점수 계산
        Integer homework = detail.getHomework() != null ? detail.getHomework() : 0;
        double homeworkPart = 0.0;
        if (policy.getHomeworkMax() > 0) {
            homeworkPart = ((double) homework / policy.getHomeworkMax()) * policy.getHomeworkWeight();
        }

        // 중간고사 점수 계산
        Integer midExam = detail.getMidExam() != null ? detail.getMidExam() : 0;
        double midtermPart = 0.0;
        if (policy.getMidtermMax() > 0) {
            midtermPart = ((double) midExam / policy.getMidtermMax()) * policy.getMidtermWeight();
        }

        // 기말고사 점수 계산
        Integer finalExam = detail.getFinalExam() != null ? detail.getFinalExam() : 0;
        double finalPart = 0.0;
        if (policy.getFinalMax() > 0) {
            finalPart = ((double) finalExam / policy.getFinalMax()) * policy.getFinalWeight();
        }

        // 최종 환산 점수 (소수점 둘째 자리까지 반올림)
        double totalScore = attendancePart + homeworkPart + midtermPart + finalPart;
        return Math.round(totalScore * 100.0) / 100.0;
    }

    /**
     * Grade 값 검증 및 변환
     * DB에 존재하지 않는 형식("A", "B", "C", "D")을 올바른 형식("A0", "B0", "C0", "D+")으로 변환
     */
    private String validateAndConvertGrade(String grade) {
        if (grade == null || grade.trim().isEmpty()) {
            throw new CustomRestfullException("등급 값이 필요합니다.", HttpStatus.BAD_REQUEST);
        }

        String gradeTrimmed = grade.trim();

        // 먼저 DB에 존재하는지 확인
        boolean exists = gradeJpaRepository.existsById(gradeTrimmed);
        if (exists) {
            return gradeTrimmed;
        }

        // DB에 존재하지 않는 경우 변환 시도
        String convertedGrade = convertGradeFormat(gradeTrimmed);
        
        // 변환된 값이 DB에 존재하는지 확인
        if (gradeJpaRepository.existsById(convertedGrade)) {
            System.out.println("⚠️ Grade 변환: " + gradeTrimmed + " -> " + convertedGrade);
            return convertedGrade;
        }

        // 변환 후에도 존재하지 않으면 에러
        throw new CustomRestfullException(
                "유효하지 않은 등급 값입니다: " + gradeTrimmed + 
                " (허용된 값: A+, A0, B+, B0, C+, C0, D+, F)", 
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Grade 형식 변환
     * "A" -> "A0", "B" -> "B0", "C" -> "C0", "D" -> "D+"
     */
    private String convertGradeFormat(String grade) {
        switch (grade.toUpperCase()) {
            case "A":
                return "A0";
            case "B":
                return "B0";
            case "C":
                return "C0";
            case "D":
                return "D+";
            default:
                return grade;
        }
    }
}