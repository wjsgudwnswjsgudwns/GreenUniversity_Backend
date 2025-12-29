package com.green.university.service;

import com.green.university.repository.AIAnalysisResultRepository;
import com.green.university.repository.StuSubDetailJpaRepository;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.TuitionJpaRepository;
import com.green.university.repository.model.*;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.NotificationJpaRepository;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.AICounseling;
import com.green.university.repository.model.StuSubDetail;
import com.green.university.repository.model.Tuition;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIAnalysisResultService {

    private final AIAnalysisResultRepository aiAnalysisResultRepository;
    private final StuSubDetailJpaRepository stuSubDetailRepository;
    private final TuitionJpaRepository tuitionRepository;
    private final AICounselingQueryService counselingQueryService;
    private final NotificationService notificationService;
    private final StudentJpaRepository studentRepository;
    private final SubjectJpaRepository subjectRepository;
    private final NotificationJpaRepository notificationRepo;

    private final MultiAIService multiAIService;

    @Autowired
    private StudentJpaRepository studentJpaRepository;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private RiskEmailService riskEmailService;

    // ===================== 기존 메서드들 (그대로 유지) =====================

    /**
     * 학생의 분석 결과 조회 - DB에서 조회
     * DB에 없으면 실시간 분석 후 저장
     */
    @Transactional
    public List<AIAnalysisResult> getStudentAnalysisResults(Integer studentId) {
        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository
                .findByStudentIdOrderByAnalyzedAtDesc(studentId);

        List<StuSubDetail> enrollments = stuSubDetailRepository.findByStudentIdWithRelations(studentId);

        if (enrollments.isEmpty()) {
            return existingResults;
        }

        Map<Integer, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.groupingBy(
                        AIAnalysisResult::getSubjectId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy((r1, r2) ->
                                        r1.getAnalyzedAt().compareTo(r2.getAnalyzedAt())
                                ),
                                opt -> opt.orElse(null)
                        )
                ));

        List<AIAnalysisResult> results = new ArrayList<>();

        for (StuSubDetail enrollment : enrollments) {
            Integer subjectId = enrollment.getSubjectId();

            if (resultMap.containsKey(subjectId)) {
                results.add(resultMap.get(subjectId));
            } else {
                AIAnalysisResult newResult = analyzeAndSaveStudent(
                        studentId,
                        subjectId,
                        enrollment.getSubject() != null ? enrollment.getSubject().getSubYear() : null,
                        enrollment.getSubject() != null ? enrollment.getSubject().getSemester() : null,
                        enrollment
                );
                results.add(newResult);
            }
        }

        return results;
    }

    /**
     * 학생-과목별 분석 수행 및 저장
     */
//    @Transactional
//    private AIAnalysisResult analyzeAndSaveStudent(Integer studentId, Integer subjectId,
//                                                   Integer year, Integer semester,
//                                                   StuSubDetail enrollment) {
//        AIAnalysisResult result = new AIAnalysisResult();
//        result.setStudentId(studentId);
//        result.setSubjectId(subjectId);
//        result.setStudent(enrollment.getStudent());
//        result.setSubject(enrollment.getSubject());
//        result.setAnalysisYear(year);
//        result.setSemester(semester);
//
//        result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
//        result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
//        result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
//        result.setFinalStatus(analyzeFinal(studentId, subjectId));
//        result.setTuitionStatus(analyzeTuition(studentId, year, semester));
//        result.setCounselingStatus(analyzeCounseling(studentId, subjectId));
//
//        result.setOverallRisk(calculateOverallRisk(result));
//
//        if ("RISK".equals(result.getOverallRisk()) || "CRITICAL".equals(result.getOverallRisk())) {
//            try {
//                String aiComment = geminiService.generateRiskComment(result, enrollment);
//                result.setAnalysisDetail(aiComment);
//            } catch (Exception e) {
//                System.err.println("AI 코멘트 생성 실패: " + e.getMessage());
//                result.setAnalysisDetail(null);
//            }
//        }
//
//        return aiAnalysisResultRepository.save(result);
//    }
    // AIAnalysisResultService.java

    @Transactional
    private AIAnalysisResult analyzeAndSaveStudent(Integer studentId, Integer subjectId,
                                                   Integer year, Integer semester,
                                                   StuSubDetail enrollment) {
        AIAnalysisResult result = new AIAnalysisResult();
        result.setStudentId(studentId);
        result.setSubjectId(subjectId);
        result.setStudent(enrollment.getStudent());
        result.setSubject(enrollment.getSubject());
        result.setAnalysisYear(year);
        result.setSemester(semester);

        // 1단계: 기존 규칙 기반 분석 (각 항목별)
        result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
        result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
        result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
        result.setFinalStatus(analyzeFinal(studentId, subjectId));
        result.setTuitionStatus(analyzeTuition(studentId, year, semester));
        result.setCounselingStatus(analyzeCounseling(studentId, subjectId));

        // 2단계: AI 종합 예측으로 최종 위험도 결정
        try {
            String aiPredictedRisk = geminiService.predictOverallDropoutRisk(result, enrollment);

            if (aiPredictedRisk != null) {
                // AI 예측 성공 - AI 판단 사용
                result.setOverallRisk(aiPredictedRisk);
                System.out.println("AI 예측 사용: " + aiPredictedRisk);
            } else {
                // AI 예측 실패 - 기존 규칙 기반 사용
                String ruleBasedRisk = calculateOverallRisk(result);
                result.setOverallRisk(ruleBasedRisk);
                System.out.println("규칙 기반 폴백: " + ruleBasedRisk);
            }
        } catch (Exception e) {
            // 예외 발생 시 안전하게 규칙 기반으로 폴백
            String ruleBasedRisk = calculateOverallRisk(result);
            result.setOverallRisk(ruleBasedRisk);
            System.err.println("AI 예측 실패, 규칙 기반 사용: " + e.getMessage());
        }

        // 3단계: RISK/CRITICAL이면 상세 코멘트 생성
        if ("RISK".equals(result.getOverallRisk()) || "CRITICAL".equals(result.getOverallRisk())) {
            try {
                String aiComment = geminiService.generateRiskComment(result, enrollment);
                result.setAnalysisDetail(aiComment);
            } catch (Exception e) {
                log.error("AI 코멘트 생성 실패: " + e.getMessage(), e);
                result.setAnalysisDetail(null);
            }
        }

        return aiAnalysisResultRepository.save(result);
    }

    /**
     * 학생-과목별 최신 분석 결과 조회
     */
    public AIAnalysisResult getLatestAnalysisResult(Integer studentId, Integer subjectId) {
        List<AIAnalysisResult> results = aiAnalysisResultRepository
                .findByStudentIdAndSubjectIdOrderByAnalyzedAtDesc(studentId, subjectId);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 과목별 위험 학생 조회
     */
    public List<AIAnalysisResult> getRiskStudentsBySubject(Integer subjectId) {
        return aiAnalysisResultRepository.findRiskStudentsBySubjectId(subjectId);
    }

    /**
     * 학과별 위험 학생 조회
     */
    public List<AIAnalysisResult> getRiskStudentsByDept(Integer deptId) {
        return aiAnalysisResultRepository.findRiskStudentsByDeptId(deptId);
    }

    /**
     * 단과대별 위험 학생 조회
     */
    public List<AIAnalysisResult> getRiskStudentsByCollege(Integer collegeId) {
        return aiAnalysisResultRepository.findRiskStudentsByCollegeId(collegeId);
    }

    /**
     * 전체 위험 학생 조회
     */
    public List<AIAnalysisResult> getAllRiskStudents() {
        return aiAnalysisResultRepository.findAllRiskStudents();
    }

    /**
     * 전체 학생 분석 결과 조회 - DB에서 조회 (기존 메서드 유지)
     */
    @Transactional(readOnly = true)
    public List<AIAnalysisResult> getAllStudents() {
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject().stream()
                .filter(e -> e.getStudent() != null && e.getSubject() != null)
                .collect(Collectors.toList());

        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findAllWithRelations();

        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) ->
                                existing.getAnalyzedAt().isAfter(replacement.getAnalyzedAt())
                                        ? existing : replacement
                ));

        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                allResults.add(resultMap.get(key));
            } else {
                if (enrollment.getStudent() == null || enrollment.getSubject() == null) {
                    continue;
                }

                AIAnalysisResult defaultResult = new AIAnalysisResult();
                defaultResult.setStudentId(enrollment.getStudentId());
                defaultResult.setSubjectId(enrollment.getSubjectId());
                defaultResult.setStudent(enrollment.getStudent());
                defaultResult.setSubject(enrollment.getSubject());

                defaultResult.setAttendanceStatus("NORMAL");
                defaultResult.setHomeworkStatus("NORMAL");
                defaultResult.setMidtermStatus("NORMAL");
                defaultResult.setFinalStatus("NORMAL");
                defaultResult.setTuitionStatus("NORMAL");
                defaultResult.setCounselingStatus("NORMAL");
                defaultResult.setOverallRisk("NORMAL");
                defaultResult.setAnalyzedAt(null);

                allResults.add(defaultResult);
            }
        }

        return allResults;
    }

    // ===================== 페이징용 새 메서드 =====================

    /**
     * 전체 학생 분석 결과 조회 (페이징) - 학생별로 그룹핑
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAllStudentsGroupedByStudent(
            Integer collegeId,
            Integer departmentId,
            String riskLevel,
            Pageable pageable) {

        // 1. 모든 분석 결과 조회 (필터링 없이)
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject().stream()
                .filter(e -> e.getStudent() != null && e.getSubject() != null)
                .collect(Collectors.toList());

        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findAllWithRelations();

        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) ->
                                existing.getAnalyzedAt().isAfter(replacement.getAnalyzedAt())
                                        ? existing : replacement
                ));

        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                allResults.add(resultMap.get(key));
            } else {
                if (enrollment.getStudent() == null || enrollment.getSubject() == null) {
                    continue;
                }

                AIAnalysisResult defaultResult = new AIAnalysisResult();
                defaultResult.setStudentId(enrollment.getStudentId());
                defaultResult.setSubjectId(enrollment.getSubjectId());
                defaultResult.setStudent(enrollment.getStudent());
                defaultResult.setSubject(enrollment.getSubject());
                defaultResult.setAttendanceStatus("NORMAL");
                defaultResult.setHomeworkStatus("NORMAL");
                defaultResult.setMidtermStatus("NORMAL");
                defaultResult.setFinalStatus("NORMAL");
                defaultResult.setTuitionStatus("NORMAL");
                defaultResult.setCounselingStatus("NORMAL");
                defaultResult.setOverallRisk("NORMAL");
                defaultResult.setAnalyzedAt(null);

                allResults.add(defaultResult);
            }
        }

        // 2. 학생별로 그룹핑
        List<Map<String, Object>> groupedStudents = groupStudentsByStudent(allResults);

        // 3. 필터링 적용
        List<Map<String, Object>> filteredStudents = groupedStudents.stream()
                .filter(student -> {
                    if (collegeId != null) {
                        Map<String, Object> studentData = (Map<String, Object>) student.get("student");
                        Map<String, Object> department = (Map<String, Object>) studentData.get("department");
                        Map<String, Object> college = (Map<String, Object>) department.get("college");
                        if (!collegeId.equals(college.get("id"))) {
                            return false;
                        }
                    }
                    if (departmentId != null) {
                        Map<String, Object> studentData = (Map<String, Object>) student.get("student");
                        Map<String, Object> department = (Map<String, Object>) studentData.get("department");
                        if (!departmentId.equals(department.get("id"))) {
                            return false;
                        }
                    }
                    if (riskLevel != null && !riskLevel.isEmpty()) {
                        if (!riskLevel.equals(student.get("highestRisk"))) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((s1, s2) -> {
                    Integer id1 = (Integer) s1.get("studentId");
                    Integer id2 = (Integer) s2.get("studentId");
                    return id1.compareTo(id2);
                })
                .collect(Collectors.toList());

        // 4. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredStudents.size());

        List<Map<String, Object>> pageContent = filteredStudents.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredStudents.size());
    }

    /**
     * 위험 학생 분석 결과 조회 (페이징) - 학생별로 그룹핑
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getRiskStudentsGroupedByStudent(
            Integer collegeId,
            Integer departmentId,
            String riskLevel,
            String searchTerm,
            Pageable pageable) {

        // 1. 모든 분석 결과 조회
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject().stream()
                .filter(e -> e.getStudent() != null && e.getSubject() != null)
                .collect(Collectors.toList());

        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findAllWithRelations();

        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) ->
                                existing.getAnalyzedAt().isAfter(replacement.getAnalyzedAt())
                                        ? existing : replacement
                ));

        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                allResults.add(resultMap.get(key));
            }
        }

        // 2. 학생별로 그룹핑
        List<Map<String, Object>> groupedStudents = groupStudentsByStudent(allResults);

        // 3. 위험 학생만 필터링 (RISK, CRITICAL)
        List<Map<String, Object>> riskStudents = groupedStudents.stream()
                .filter(student -> {
                    String risk = (String) student.get("highestRisk");
                    return "RISK".equals(risk) || "CRITICAL".equals(risk);
                })
                .collect(Collectors.toList());

        // 4. 필터링 적용 (단과대학, 학과, 위험도, 검색어)
        List<Map<String, Object>> filteredStudents = riskStudents.stream()
                .filter(student -> {
                    if (collegeId != null) {
                        Map<String, Object> studentData = (Map<String, Object>) student.get("student");
                        if (studentData == null) return false;
                        Map<String, Object> department = (Map<String, Object>) studentData.get("department");
                        if (department == null) return false;
                        Map<String, Object> college = (Map<String, Object>) department.get("college");
                        if (college == null) return false;
                        if (!collegeId.equals(college.get("id"))) {
                            return false;
                        }
                    }
                    if (departmentId != null) {
                        Map<String, Object> studentData = (Map<String, Object>) student.get("student");
                        if (studentData == null) return false;
                        Map<String, Object> department = (Map<String, Object>) studentData.get("department");
                        if (department == null) return false;
                        if (!departmentId.equals(department.get("id"))) {
                            return false;
                        }
                    }
                    if (riskLevel != null && !riskLevel.isEmpty()) {
                        if (!riskLevel.equals(student.get("highestRisk"))) {
                            return false;
                        }
                    }
                    if (searchTerm != null && !searchTerm.isEmpty()) {
                        String term = searchTerm.toLowerCase();
                        Integer studentId = (Integer) student.get("studentId");
                        Map<String, Object> studentData = (Map<String, Object>) student.get("student");
                        if (studentData == null) return false;
                        String name = (String) studentData.get("name");
                        Map<String, Object> department = (Map<String, Object>) studentData.get("department");
                        String deptName = department != null ? (String) department.get("name") : "";

                        boolean matches = String.valueOf(studentId).toLowerCase().contains(term) ||
                                (name != null && name.toLowerCase().contains(term)) ||
                                (deptName != null && deptName.toLowerCase().contains(term));
                        if (!matches) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((s1, s2) -> {
                    Integer id1 = (Integer) s1.get("studentId");
                    Integer id2 = (Integer) s2.get("studentId");
                    return id1.compareTo(id2);
                })
                .collect(Collectors.toList());

        // 5. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredStudents.size());

        List<Map<String, Object>> pageContent = filteredStudents.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredStudents.size());
    }

    /**
     * 학생별로 그룹핑하는 헬퍼 메서드
     */
    private List<Map<String, Object>> groupStudentsByStudent(List<AIAnalysisResult> analysisResults) {
        Map<Integer, Map<String, Object>> studentMap = new java.util.HashMap<>();

        for (AIAnalysisResult result : analysisResults) {
            Integer studentId = result.getStudentId();

            if (!studentMap.containsKey(studentId)) {
                Map<String, Object> studentData = new java.util.HashMap<>();
                studentData.put("studentId", studentId);
                studentData.put("student", convertStudentToMap(result.getStudent()));
                studentData.put("subjects", new ArrayList<AIAnalysisResult>());
                studentData.put("highestRisk", "NORMAL");
                studentData.put("riskPriority", 0);
                studentData.put("criticalSubjects", new ArrayList<AIAnalysisResult>());
                studentData.put("riskSubjects", new ArrayList<AIAnalysisResult>());
                studentMap.put(studentId, studentData);
            }

            Map<String, Object> studentData = studentMap.get(studentId);
            ((List<AIAnalysisResult>) studentData.get("subjects")).add(result);

            if ("CRITICAL".equals(result.getOverallRisk())) {
                ((List<AIAnalysisResult>) studentData.get("criticalSubjects")).add(result);
            } else if ("RISK".equals(result.getOverallRisk())) {
                ((List<AIAnalysisResult>) studentData.get("riskSubjects")).add(result);
            }

            int riskPriority = getRiskPriority(result.getOverallRisk());
            if (riskPriority > (Integer) studentData.get("riskPriority")) {
                studentData.put("highestRisk", result.getOverallRisk());
                studentData.put("riskPriority", riskPriority);
            }
        }

        return new ArrayList<>(studentMap.values());
    }

    private Map<String, Object> convertStudentToMap(Student student) {
        if (student == null) return null;

        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", student.getId());
        map.put("name", student.getName());
        map.put("grade", student.getGrade());

        if (student.getDepartment() != null) {
            Map<String, Object> deptMap = new java.util.HashMap<>();
            deptMap.put("id", student.getDepartment().getId());
            deptMap.put("name", student.getDepartment().getName());

            if (student.getDepartment().getCollege() != null) {
                Map<String, Object> collegeMap = new java.util.HashMap<>();
                collegeMap.put("id", student.getDepartment().getCollege().getId());
                collegeMap.put("name", student.getDepartment().getCollege().getName());
                deptMap.put("college", collegeMap);
            }

            map.put("department", deptMap);
        }

        return map;
    }

    private int getRiskPriority(String risk) {
        switch (risk) {
            case "CRITICAL": return 4;
            case "RISK": return 3;
            case "CAUTION": return 2;
            case "NORMAL": return 1;
            default: return 0;
        }
    }

    // ===================== 기존 분석 메서드들 (그대로 유지) =====================

    /**
     * AI 분석 실행 - DB에 저장
     */
//    @Transactional
//    public AIAnalysisResult analyzeStudent(Integer studentId, Integer subjectId,
//                                           Integer year, Integer semester) {
//        AIAnalysisResult existingResult = getLatestAnalysisResult(studentId, subjectId);
//
//        StuSubDetail detail = stuSubDetailRepository
//                .findByStudentIdAndSubjectId(studentId, subjectId)
//                .orElse(null);
//
//        AIAnalysisResult result;
//        if (existingResult != null &&
//                existingResult.getAnalyzedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
//            result = existingResult;
//        } else {
//            result = new AIAnalysisResult();
//            result.setStudentId(studentId);
//            result.setSubjectId(subjectId);
//            result.setStudent(detail.getStudent());
//            result.setSubject(detail.getSubject());
//            result.setAnalysisYear(year);
//            result.setSemester(semester);
//        }
//
//        result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
//        result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
//        result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
//        result.setFinalStatus(analyzeFinal(studentId, subjectId));
//        result.setTuitionStatus(analyzeTuition(studentId, year, semester));
//        result.setCounselingStatus(analyzeCounseling(studentId, subjectId));
//
//        String previousRisk = result.getOverallRisk();
//        String newRisk = calculateOverallRisk(result);
//        result.setOverallRisk(newRisk);
//
//        if ("RISK".equals(newRisk) || "CRITICAL".equals(newRisk)) {
//            try {
//                String aiComment = geminiService.generateRiskComment(result, detail);
//                result.setAnalysisDetail(aiComment);
//            } catch (Exception e) {
//                log.error("AI 코멘트 생성 실패: " + e.getMessage(), e);
//                result.setAnalysisDetail(null);
//            }
//        } else {
//            result.setAnalysisDetail(null);
//        }
//
//        AIAnalysisResult saved = aiAnalysisResultRepository.save(result);
//
//        log.info("위험도 분석 결과: 학생 ID={}, 과목 ID={}, 이전 위험도={}, 새 위험도={}",
//                studentId, subjectId, previousRisk, newRisk);
//
//        if (newRisk.equals("RISK") || newRisk.equals("CRITICAL")) {
//            log.info("위험 알림 발송: 학생 ID={}, 과목 ID={}, 위험도={}",
//                    studentId, subjectId, newRisk);
//            sendRiskNotifications(saved, newRisk);
//        } else {
//            log.debug("위험도가 NORMAL 또는 CAUTION: 학생 ID={}, 과목 ID={}, 위험도={}",
//                    studentId, subjectId, newRisk);
//        }
//
//        return saved;
//    }

    /**
     * AI 분석 실행 - MultiAI 버전
     */
    @Transactional
    public AIAnalysisResult analyzeStudent(Integer studentId, Integer subjectId,
                                           Integer year, Integer semester) {
        long startTime = System.currentTimeMillis();

        AIAnalysisResult existingResult = getLatestAnalysisResult(studentId, subjectId);
        StuSubDetail detail = stuSubDetailRepository
                .findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        AIAnalysisResult result;
        if (existingResult != null) {
            result = existingResult;
        } else {
            result = new AIAnalysisResult();
            result.setStudentId(studentId);
            result.setSubjectId(subjectId);
            result.setStudent(detail != null ? detail.getStudent() : null);
            result.setSubject(detail != null ? detail.getSubject() : null);
            result.setAnalysisYear(year);
            result.setSemester(semester);
        }

        // 1단계: 규칙 기반 각 항목별 분석
        result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
        result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
        result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
        result.setFinalStatus(analyzeFinal(studentId, subjectId));
        result.setTuitionStatus(analyzeTuition(studentId, year, semester));
        result.setCounselingStatus(analyzeCounseling(studentId, subjectId));

        String previousRisk = result.getOverallRisk();

        // 2단계: 규칙 기반 위험도도 계산 (검증용)
        String ruleBasedRisk = calculateOverallRisk(result);

        // 3단계: AI 종합 예측
        String aiPredictedRisk = null;
        try {
            log.info("🤖 AI 종합 예측 시작: 학생={}, 과목={}", studentId, subjectId);
            aiPredictedRisk = multiAIService.predictOverallDropoutRisk(result, detail);

            if (aiPredictedRisk != null && isValidRiskLevel(aiPredictedRisk)) {
                log.info("✅ AI 예측: {}", aiPredictedRisk);
            } else {
                log.warn("⚠️ AI 예측 실패");
                aiPredictedRisk = null;
            }
        } catch (Exception e) {
            log.error("❌ AI 예측 에러: {}", e.getMessage());
            aiPredictedRisk = null;
        }

        // 4단계: ⭐ AI 판단 검증 및 보정
        String finalRisk;
        if (aiPredictedRisk != null) {
            // AI 판단이 규칙 기반보다 과도하게 낮거나 높으면 보정
            finalRisk = validateAndCorrectAIPrediction(
                    aiPredictedRisk,
                    ruleBasedRisk,
                    result,
                    detail
            );

            if (!finalRisk.equals(aiPredictedRisk)) {
                log.warn("⚠️ AI 판단 보정: AI={} → 최종={} (규칙={}, 이유=검증 실패)",
                        aiPredictedRisk, finalRisk, ruleBasedRisk);
            }
        } else {
            // AI 실패 시 규칙 기반 사용
            finalRisk = ruleBasedRisk;
            log.warn("⚠️ AI 실패, 규칙 기반 사용: {}", ruleBasedRisk);
        }

        result.setOverallRisk(finalRisk);

        // 5단계: RISK/CRITICAL만 AI 상세 코멘트
        if ("RISK".equals(finalRisk) || "CRITICAL".equals(finalRisk)) {
            try {
                String aiComment = multiAIService.generateRiskComment(result, detail);
                result.setAnalysisDetail(aiComment);
            } catch (Exception e) {
                log.warn("⚠️ AI 코멘트 실패: {}", e.getMessage());
                result.setAnalysisDetail(generateFallbackComment(result, detail));
            }
        } else {
            result.setAnalysisDetail(null);
        }

        AIAnalysisResult saved = aiAnalysisResultRepository.save(result);

        log.info("📊 분석 완료: 학생={}, 과목={}, AI={}, 규칙={}, 최종={}, 소요={}ms",
                studentId, subjectId, aiPredictedRisk, ruleBasedRisk, finalRisk,
                System.currentTimeMillis() - startTime);

        if ("RISK".equals(finalRisk) || "CRITICAL".equals(finalRisk)) {
            sendRiskNotifications(saved, finalRisk);
        }

        return saved;
    }

    /**
     * ⭐ AI 판단 검증 및 보정 로직
     *
     * AI가 명백히 잘못 판단한 경우 규칙 기반으로 보정
     */
    private String validateAndCorrectAIPrediction(
            String aiRisk,
            String ruleBasedRisk,
            AIAnalysisResult result,
            StuSubDetail detail) {

        // 각 상태 카운트
        int criticalCount = 0;
        int riskCount = 0;
        int cautionCount = 0;
        int normalCount = 0;

        String[] statuses = {
                result.getAttendanceStatus(),
                result.getHomeworkStatus(),
                result.getMidtermStatus(),
                result.getFinalStatus(),
                result.getTuitionStatus(),
                result.getCounselingStatus()
        };

        for (String status : statuses) {
            if (status == null) continue;
            switch (status) {
                case "CRITICAL": criticalCount++; break;
                case "RISK": riskCount++; break;
                case "CAUTION": cautionCount++; break;
                case "NORMAL": normalCount++; break;
            }
        }

        // ===== 검증 규칙 =====

        // 규칙 1: CRITICAL 1개 이상 있는데 AI가 NORMAL/CAUTION → 보정
        if (criticalCount >= 1 && ("NORMAL".equals(aiRisk) || "CAUTION".equals(aiRisk))) {
            log.warn("⚠️ 검증 실패: CRITICAL {}개 있는데 AI={} → CRITICAL로 보정",
                    criticalCount, aiRisk);
            return "CRITICAL";
        }

        // 규칙 2: 모두 NORMAL인데 AI가 CRITICAL/RISK → 보정
        if (normalCount == 6 && ("CRITICAL".equals(aiRisk) || "RISK".equals(aiRisk))) {
            log.warn("⚠️ 검증 실패: 모두 NORMAL인데 AI={} → NORMAL로 보정", aiRisk);
            return "NORMAL";
        }

        // 규칙 3: 등록금만 CAUTION이고 나머지 NORMAL인데 AI가 CRITICAL → 보정
        if ("CAUTION".equals(result.getTuitionStatus()) &&
                "NORMAL".equals(result.getAttendanceStatus()) &&
                "NORMAL".equals(result.getHomeworkStatus()) &&
                "NORMAL".equals(result.getMidtermStatus()) &&
                "NORMAL".equals(result.getFinalStatus()) &&
                "NORMAL".equals(result.getCounselingStatus()) &&
                "CRITICAL".equals(aiRisk)) {

            log.warn("⚠️ 검증 실패: 등록금만 CAUTION인데 AI=CRITICAL → CAUTION으로 보정");
            return "CAUTION";
        }

        // 규칙 4: F학점 확정 (환산 결석 3회 이상)인데 AI가 NORMAL/CAUTION → 보정
        if (detail != null) {
            int absent = detail.getAbsent() != null ? detail.getAbsent() : 0;
            int lateness = detail.getLateness() != null ? detail.getLateness() : 0;
            double totalAbsent = absent + (lateness / 3.0);

            if (totalAbsent >= 3.0 && ("NORMAL".equals(aiRisk) || "CAUTION".equals(aiRisk))) {
                log.warn("⚠️ 검증 실패: F학점 확정 (환산결석 {})인데 AI={} → CRITICAL로 보정",
                        totalAbsent, aiRisk);
                return "CRITICAL";
            }
        }

        // 규칙 5: RISK 2개 이상인데 AI가 NORMAL → 보정
        if (riskCount >= 2 && "NORMAL".equals(aiRisk)) {
            log.warn("⚠️ 검증 실패: RISK {}개인데 AI=NORMAL → RISK로 보정", riskCount);
            return "RISK";
        }

        // 규칙 6: AI와 규칙 기반 차이가 2단계 이상 → 규칙 기반 우선
        int aiLevel = getRiskPriority(aiRisk);
        int ruleLevel = getRiskPriority(ruleBasedRisk);

        if (Math.abs(aiLevel - ruleLevel) >= 2) {
            log.warn("⚠️ 검증 실패: AI({})와 규칙({}) 차이 2단계 이상 → 규칙 우선",
                    aiRisk, ruleBasedRisk);
            return ruleBasedRisk;
        }

        // 검증 통과 - AI 판단 사용
        return aiRisk;
    }



    /**
     * 전체 학생-과목에 대한 일괄 AI 분석 실행 (Rate Limit 고려)
     */
    @Transactional
    public int analyzeAllStudentsAndSubjects(Integer year, Integer semester) {
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject();

        int successCount = 0;
        int apiCallCount = 0;
        int riskCount = 0;
        int normalCount = 0;

        // Gemini 무료 tier: 분당 15개 제한
        int maxApiCallsPerMinute = 12; // 안전 마진
        long startTime = System.currentTimeMillis();
        long lastBatchTime = startTime;

        log.info("📊 총 {}개의 학생-과목 AI 분석 시작 (최적화 버전)", allEnrollments.size());

        for (int i = 0; i < allEnrollments.size(); i++) {
            StuSubDetail enrollment = allEnrollments.get(i);

            try {
                // API 호출 횟수 체크
                if (apiCallCount >= maxApiCallsPerMinute) {
                    long elapsed = System.currentTimeMillis() - lastBatchTime;
                    long waitTime = 60000 - elapsed; // 1분 - 경과 시간

                    if (waitTime > 0) {
                        log.info("⏱️ Rate Limit 방지 대기: {}초...", waitTime / 1000);
                        Thread.sleep(waitTime);
                    }

                    apiCallCount = 0;
                    lastBatchTime = System.currentTimeMillis();
                }

                AIAnalysisResult result = analyzeStudent(
                        enrollment.getStudentId(),
                        enrollment.getSubjectId(),
                        year != null ? year :
                                (enrollment.getSubject() != null ?
                                        enrollment.getSubject().getSubYear() : null),
                        semester != null ? semester :
                                (enrollment.getSubject() != null ?
                                        enrollment.getSubject().getSemester() : null)
                );

                successCount++;

                // API 호출 카운트
                apiCallCount++; // AI 예측 1회

                if ("RISK".equals(result.getOverallRisk()) ||
                        "CRITICAL".equals(result.getOverallRisk())) {
                    riskCount++;
                    apiCallCount++; // AI 코멘트 1회
                } else {
                    normalCount++;
                }

                // 진행 상황 로그
                if ((i + 1) % 5 == 0 || (i + 1) == allEnrollments.size()) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double avgTime = elapsed / (double) successCount;
                    long estimatedRemaining = (long) (avgTime * (allEnrollments.size() - successCount));

                    log.info("📈 진행: {}/{}명 | NORMAL: {}명, RISK+: {}명 | " +
                                    "API: {}회 | 평균: {}ms/건 | 예상 남은 시간: {}초",
                            successCount, allEnrollments.size(),
                            normalCount, riskCount,
                            apiCallCount,
                            String.format("%.0f", avgTime),
                            estimatedRemaining / 1000);
                }

                // 배치 간 짧은 대기 (0.5초)
                Thread.sleep(500);

            } catch (Exception e) {
                log.error("학생 {}, 과목 {} 분석 실패: {}",
                        enrollment.getStudentId(), enrollment.getSubjectId(), e.getMessage());

                // Rate limit 에러면 중단
                if (e.getMessage() != null && e.getMessage().contains("할당량")) {
                    log.error("❌ API 할당량 초과로 배치 분석 중단");
                    break;
                }
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;

        log.info("✅ 배치 분석 완료: {}/{}명 성공 | " +
                        "NORMAL: {}명, RISK+: {}명 | " +
                        "총 소요: {}초 (평균 {}/건)",
                successCount, allEnrollments.size(),
                normalCount, riskCount,
                totalTime / 1000,
                String.format("%.1f초", totalTime / 1000.0 / successCount));

        return successCount;
    }


    /**
     * 위험도 레벨 유효성 검증
     */
    private boolean isValidRiskLevel(String riskLevel) {
        if (riskLevel == null) {
            return false;
        }
        return riskLevel.equals("NORMAL") ||
                riskLevel.equals("CAUTION") ||
                riskLevel.equals("RISK") ||
                riskLevel.equals("CRITICAL");
    }
    /**
     * AI 코멘트 생성 실패 시 폴백 메시지
     */
    private String generateFallbackComment(AIAnalysisResult result, StuSubDetail detail) {
        StringBuilder comment = new StringBuilder();
        List<String> issues = new ArrayList<>();

        if (!"NORMAL".equals(result.getAttendanceStatus())) {
            int absent = detail != null && detail.getAbsent() != null ? detail.getAbsent() : 0;
            int lateness = detail != null && detail.getLateness() != null ? detail.getLateness() : 0;
            issues.add(String.format("출석 문제 (결석 %d회, 지각 %d회)", absent, lateness));
        }

        if (!"NORMAL".equals(result.getHomeworkStatus())) {
            int homework = detail != null && detail.getHomework() != null ? detail.getHomework() : 0;
            issues.add(String.format("과제 미흡 (%d점)", homework));
        }

        if (!"NORMAL".equals(result.getMidtermStatus())) {
            int midExam = detail != null && detail.getMidExam() != null ? detail.getMidExam() : 0;
            issues.add(String.format("중간고사 저조 (%d점)", midExam));
        }

        if (!"NORMAL".equals(result.getFinalStatus())) {
            int finalExam = detail != null && detail.getFinalExam() != null ? detail.getFinalExam() : 0;
            issues.add(String.format("기말고사 저조 (%d점)", finalExam));
        }

        if (!"NORMAL".equals(result.getTuitionStatus())) {
            issues.add("등록금 미납");
        }

        if (!"NORMAL".equals(result.getCounselingStatus())) {
            issues.add("상담 내용에서 위험 신호 감지");
        }

        if (issues.isEmpty()) {
            return "모니터링이 필요한 학생입니다.";
        }

        comment.append("다음 영역에서 문제가 감지되었습니다: ");
        comment.append(String.join(", ", issues));
        comment.append(". 즉각적인 학습 지원과 상담이 필요합니다.");

        return comment.toString();
    }

    /**
     * 출결 분석
     */
    private String analyzeAttendance(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        if (detail == null) {
            return "NORMAL";
        }

        if (detail.getAbsent() == null && detail.getLateness() == null) {
            return "NORMAL";
        }

        int absent = detail.getAbsent() != null ? detail.getAbsent() : 0;
        int lateness = detail.getLateness() != null ? detail.getLateness() : 0;

        double totalAbsent = absent + (lateness / 3.0);

        if (totalAbsent >= 3) {
            return "CRITICAL";
        } else if (totalAbsent >= 2) {
            return "RISK";
        } else if (totalAbsent >= 1) {
            return "CAUTION";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 과제 분석
     */
    private String analyzeHomework(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        if (detail == null || detail.getHomework() == null) {
            return "NORMAL";
        }

        int homework = detail.getHomework();

        if (homework >= 80) {
            return "NORMAL";
        } else if (homework >= 60) {
            return "CAUTION";
        } else if (homework >= 40) {
            return "RISK";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 중간고사 분석
     */
    private String analyzeMidterm(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        if (detail == null || detail.getMidExam() == null) {
            return "NORMAL";
        }

        int midExam = detail.getMidExam();

        if (midExam >= 70) {
            return "NORMAL";
        } else if (midExam >= 50) {
            return "CAUTION";
        } else if (midExam >= 30) {
            return "RISK";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 기말고사 분석
     */
    private String analyzeFinal(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        if (detail == null || detail.getFinalExam() == null) {
            return "NORMAL";
        }

        int finalExam = detail.getFinalExam();

        if (finalExam >= 70) {
            return "NORMAL";
        } else if (finalExam >= 50) {
            return "CAUTION";
        } else if (finalExam >= 30) {
            return "RISK";
        } else {
            return "CRITICAL";
        }
    }

    /**
     * 등록금 분석
     */
    private String analyzeTuition(Integer studentId, Integer year, Integer semester) {
        Optional<Tuition> tuitionOpt = tuitionRepository
                .findByIdStudentIdAndIdTuiYearAndIdSemester(studentId, year, semester);

        if (tuitionOpt.isEmpty()) {
            return "NORMAL";
        }

        Tuition tuition = tuitionOpt.get();

        if (tuition.getStatus() == null || !tuition.getStatus()) {
            return "CAUTION";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 상담 분석
     */
    private String analyzeCounseling(Integer studentId, Integer subjectId) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        List<AICounseling> counselings =
                counselingQueryService.getCompletedCounselingsForAnalysisBySubject(studentId, subjectId);

        List<AICounseling> recentCounselings = counselings.stream()
                .filter(c -> c.getCompletedAt() != null &&
                        c.getCompletedAt().isAfter(threeMonthsAgo))
                .toList();

        if (recentCounselings.isEmpty()) {
            return "NORMAL";
        }

        int frequencyScore = calculateFrequencyScore(recentCounselings.size());
        int trendScore = calculateTrendScore(recentCounselings);

        int totalScore = (frequencyScore * 30 + trendScore * 70) / 100;

        if (totalScore >= 80) return "CRITICAL";
        if (totalScore >= 60) return "RISK";
        if (totalScore >= 40) return "CAUTION";
        return "NORMAL";
    }


    private int calculateFrequencyScore(int counselingCount) {
        if (counselingCount >= 10) {
            return 100;
        } else if (counselingCount >= 8) {
            return 85;
        } else if (counselingCount >= 6) {
            return 70;
        } else if (counselingCount >= 5) {
            return 55;
        } else if (counselingCount >= 4) {
            return 40;
        } else if (counselingCount >= 3) {
            return 25;
        } else if (counselingCount >= 2) {
            return 15;
        } else {
            return 10;
        }
    }

    private int calculateTrendScore(List<AICounseling> counselings) {
        if (counselings.isEmpty()) {
            return 0;
        }

        int analyzeCount = Math.min(3, counselings.size());
        List<AICounseling> recentForTrend = counselings.subList(0, analyzeCount);

        List<Integer> riskLevels = new ArrayList<>();
        for (AICounseling counseling : recentForTrend) {
            riskLevels.add(getRiskLevel(counseling.getAiAnalysisResult()));
        }

        double weightedScore = 0;
        double totalWeight = 0;

        for (int i = 0; i < riskLevels.size(); i++) {
            double weight = 1.0 / (i + 1);
            weightedScore += riskLevels.get(i) * 25 * weight;
            totalWeight += weight;
        }

        int baseScore = (int) (weightedScore / totalWeight);
        int trendAdjustment = 0;

        if (riskLevels.size() >= 2) {
            int latest = riskLevels.get(0);
            int previous = riskLevels.get(1);

            if (latest > previous) {
                trendAdjustment = 15;
            } else if (latest < previous) {
                trendAdjustment = -15;
            }

            if (riskLevels.size() >= 3) {
                int beforePrevious = riskLevels.get(2);

                if (latest > previous && previous > beforePrevious) {
                    trendAdjustment = 25;
                } else if (latest < previous && previous < beforePrevious) {
                    trendAdjustment = -20;
                } else if (latest == previous && previous != beforePrevious) {
                    trendAdjustment = 10;
                }
            }
        }

        if ("CRITICAL".equals(counselings.get(0).getAiAnalysisResult())) {
            return Math.max(baseScore + trendAdjustment, 85);
        }

        int finalScore = baseScore + trendAdjustment;
        return Math.max(0, Math.min(100, finalScore));
    }

    private int getRiskLevel(String riskStatus) {
        if (riskStatus == null) {
            return 1;
        }

        switch (riskStatus) {
            case "CRITICAL":
                return 4;
            case "RISK":
                return 3;
            case "CAUTION":
                return 2;
            case "NORMAL":
            default:
                return 1;
        }
    }

    /**
     * 종합 위험도 계산 - 개선된 로직
     * 더 합리적이고 일관성 있는 판단 기준 적용
     */
    private String calculateOverallRisk(AIAnalysisResult result) {
        int criticalCount = 0;
        int riskCount = 0;
        int cautionCount = 0;

        String[] statuses = {
                result.getAttendanceStatus(),
                result.getHomeworkStatus(),
                result.getMidtermStatus(),
                result.getFinalStatus(),
                result.getTuitionStatus(),
                result.getCounselingStatus()
        };

        for (String status : statuses) {
            if (status == null) continue;

            switch (status) {
                case "CRITICAL":
                    criticalCount++;
                    break;
                case "RISK":
                    riskCount++;
                    break;
                case "CAUTION":
                    cautionCount++;
                    break;
            }
        }

        // 규칙 기반 판정
        if (criticalCount >= 1) {
            return "CRITICAL";
        } else if (riskCount >= 2) {
            return "RISK";
        } else if (riskCount >= 1 || cautionCount >= 3) {
            return "CAUTION";
        } else {
            return "NORMAL";
        }
    }



    /**
     * 교수 담당 학생의 분석 결과 조회 - DB 조회
     */
    @Transactional(readOnly = true)
    public List<AIAnalysisResult> getAdvisorStudents(Integer advisorId) {
        List<Student> advisorStudents = studentJpaRepository.findByAdvisorId(advisorId);

        if (advisorStudents.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> studentIds = advisorStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject().stream()
                .filter(e -> studentIds.contains(e.getStudentId()))
                .filter(e -> e.getStudent() != null && e.getSubject() != null)
                .collect(Collectors.toList());

        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findByAdvisorIdWithRelations(advisorId);

        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) ->
                                existing.getAnalyzedAt().isAfter(replacement.getAnalyzedAt())
                                        ? existing : replacement
                ));

        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                allResults.add(resultMap.get(key));
            } else {
                if (enrollment.getStudent() == null || enrollment.getSubject() == null) {
                    continue;
                }

                AIAnalysisResult defaultResult = new AIAnalysisResult();
                defaultResult.setStudentId(enrollment.getStudentId());
                defaultResult.setSubjectId(enrollment.getSubjectId());
                defaultResult.setStudent(enrollment.getStudent());
                defaultResult.setSubject(enrollment.getSubject());

                defaultResult.setAttendanceStatus("NORMAL");
                defaultResult.setHomeworkStatus("NORMAL");
                defaultResult.setMidtermStatus("NORMAL");
                defaultResult.setFinalStatus("NORMAL");
                defaultResult.setTuitionStatus("NORMAL");
                defaultResult.setCounselingStatus("NORMAL");
                defaultResult.setOverallRisk("NORMAL");
                defaultResult.setAnalyzedAt(null);

                allResults.add(defaultResult);
            }
        }

        return allResults;
    }

    /**
     * 교수 담당 학생 분석 결과 조회 (페이징) - 학생별로 그룹핑
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAdvisorStudentsGroupedByStudent(
            Integer advisorId,
            String riskLevel,
            Pageable pageable) {

        // 1. 담당 학생 조회
        List<Student> advisorStudents = studentJpaRepository.findByAdvisorId(advisorId);

        if (advisorStudents.isEmpty()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        List<Integer> studentIds = advisorStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        // 2. 모든 분석 결과 조회
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject().stream()
                .filter(e -> studentIds.contains(e.getStudentId()))
                .filter(e -> e.getStudent() != null && e.getSubject() != null)
                .collect(Collectors.toList());

        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findByAdvisorIdWithRelations(advisorId);

        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) ->
                                existing.getAnalyzedAt().isAfter(replacement.getAnalyzedAt())
                                        ? existing : replacement
                ));

        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                allResults.add(resultMap.get(key));
            } else {
                if (enrollment.getStudent() == null || enrollment.getSubject() == null) {
                    continue;
                }

                AIAnalysisResult defaultResult = new AIAnalysisResult();
                defaultResult.setStudentId(enrollment.getStudentId());
                defaultResult.setSubjectId(enrollment.getSubjectId());
                defaultResult.setStudent(enrollment.getStudent());
                defaultResult.setSubject(enrollment.getSubject());
                defaultResult.setAttendanceStatus("NORMAL");
                defaultResult.setHomeworkStatus("NORMAL");
                defaultResult.setMidtermStatus("NORMAL");
                defaultResult.setFinalStatus("NORMAL");
                defaultResult.setTuitionStatus("NORMAL");
                defaultResult.setCounselingStatus("NORMAL");
                defaultResult.setOverallRisk("NORMAL");
                defaultResult.setAnalyzedAt(null);

                allResults.add(defaultResult);
            }
        }

        // 3. 학생별로 그룹핑
        List<Map<String, Object>> groupedStudents = groupStudentsByStudent(allResults);

        // 4. 필터링 적용
        List<Map<String, Object>> filteredStudents = groupedStudents.stream()
                .filter(student -> {
                    if (riskLevel != null && !riskLevel.isEmpty()) {
                        if (!riskLevel.equals(student.get("highestRisk"))) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted((s1, s2) -> {
                    Integer id1 = (Integer) s1.get("studentId");
                    Integer id2 = (Integer) s2.get("studentId");
                    return id1.compareTo(id2);
                })
                .collect(Collectors.toList());

        // 5. 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filteredStudents.size());

        List<Map<String, Object>> pageContent = filteredStudents.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredStudents.size());
    }

    /**
     * 위험 알림 발송 (시스템 알림 + 이메일)
     * 위험도가 변경되어 RISK 또는 CRITICAL이 되었을 때 호출됨
     */
    private void sendRiskNotifications(AIAnalysisResult result, String riskLevel) {
        try {
            Integer studentId = result.getStudentId();
            Integer subjectId = result.getSubjectId();

            if (studentId == null || subjectId == null) {
                log.warn("학생 ID 또는 과목 ID가 null입니다. 알림 발송 건너뜀.");
                return;
            }

            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                log.warn("학생을 찾을 수 없습니다. ID: {}", studentId);
                return;
            }

            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            if (subject == null) {
                log.warn("과목을 찾을 수 없습니다. ID: {}", subjectId);
                return;
            }

            String studentName = student.getName();
            String subjectName = subject.getName();
            String riskLabel = riskLevel.equals("CRITICAL") ? "심각" : "위험";

            // 1. 시스템 알림 발송 (기존 로직 유지)
            boolean studentNotifiedToday = notificationRepo.existsByUserIdAndTypeAndToday(
                    studentId, "STUDENT_RISK_ALERT");

            if (!studentNotifiedToday) {
                String studentMessage = String.format(
                        "%s 과목에서 %s 상태가 감지되었습니다. 상담을 받으시기 바랍니다.",
                        subjectName,
                        riskLabel
                );
                notificationService.createNotification(
                        studentId,
                        "STUDENT_RISK_ALERT",
                        studentMessage,
                        null
                );
                log.info("학생에게 시스템 알림 발송: 학생={}, 과목={}, 위험도={}",
                        studentName, subjectName, riskLevel);
            } else {
                log.info("학생에게 오늘 이미 시스템 알림을 보냈으므로 건너뜀: 학생 ID={}", studentId);
            }

            if (subject.getProfessor() != null) {
                Integer professorId = subject.getProfessor().getId();
                String professorName = subject.getProfessor().getName();

                String professorMessage = String.format(
                        "%s 학생이 %s 과목에서 %s 상태입니다. 상담이 필요합니다.",
                        studentName,
                        subjectName,
                        riskLabel
                );
                notificationService.createNotification(
                        professorId,
                        "PROFESSOR_RISK_ALERT",
                        professorMessage,
                        null
                );
                log.info("교수에게 시스템 알림 발송: 교수={}, 학생={}, 과목={}, 위험도={}",
                        professorName, studentName, subjectName, riskLevel);
            }

            // 2. 이메일 발송 (새로운 기능)
            try {
                // 학생에게 이메일 발송
                riskEmailService.sendRiskEmailToStudent(student, subject, riskLevel, result);
                log.info("학생 이메일 발송 완료: 학생={}, 이메일={}",
                        studentName, student.getEmail());

                // 지도교수에게 이메일 발송
                if (student.getAdvisor() != null) {
                    riskEmailService.sendRiskEmailToProfessor(student, subject, riskLevel, result);
                    log.info("지도교수 이메일 발송 완료: 교수={}, 이메일={}",
                            student.getAdvisor().getName(), student.getAdvisor().getEmail());
                } else {
                    log.warn("학생의 지도교수 정보가 없습니다. 학생 ID: {}", studentId);
                }
            } catch (Exception e) {
                log.error("이메일 발송 중 오류 발생: " + e.getMessage(), e);
                // 이메일 발송 실패해도 시스템 알림은 정상 발송되도록 예외를 잡음
            }

        } catch (Exception e) {
            log.error("위험 알림 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 학생의 모든 과목 일괄 분석 (일관성 보장)
     * 한 학생의 모든 과목을 동시에 분석하여 일관된 기준 적용
     */
    @Transactional
    public List<AIAnalysisResult> analyzeStudentAllSubjects(Integer studentId,
                                                            Integer year,
                                                            Integer semester) {
        log.info("📊 학생 전체 과목 분석 시작: 학생 ID={}", studentId);

        // 해당 학생의 모든 수강 과목 조회
        List<StuSubDetail> enrollments = stuSubDetailRepository
                .findByStudentIdWithRelations(studentId);

        if (enrollments.isEmpty()) {
            log.warn("⚠️ 수강 과목이 없습니다: 학생 ID={}", studentId);
            return new ArrayList<>();
        }

        List<AIAnalysisResult> results = new ArrayList<>();
        boolean aiAvailable = true; // AI 사용 가능 여부
        String fallbackMethod = null; // 폴백 사용 시 어떤 방법 사용했는지

        for (int i = 0; i < enrollments.size(); i++) {
            StuSubDetail enrollment = enrollments.get(i);
            Integer subjectId = enrollment.getSubjectId();

            log.info("📝 과목 분석 [{}/{}]: 학생 ID={}, 과목 ID={}, 과목명={}",
                    i + 1, enrollments.size(), studentId, subjectId,
                    enrollment.getSubject() != null ? enrollment.getSubject().getName() : "N/A");

            AIAnalysisResult result = new AIAnalysisResult();
            result.setStudentId(studentId);
            result.setSubjectId(subjectId);
            result.setStudent(enrollment.getStudent());
            result.setSubject(enrollment.getSubject());
            result.setAnalysisYear(year != null ? year :
                    (enrollment.getSubject() != null ? enrollment.getSubject().getSubYear() : null));
            result.setSemester(semester != null ? semester :
                    (enrollment.getSubject() != null ? enrollment.getSubject().getSemester() : null));

            // 각 항목별 분석
            result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
            result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
            result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
            result.setFinalStatus(analyzeFinal(studentId, subjectId));
            result.setTuitionStatus(analyzeTuition(studentId,
                    result.getAnalysisYear(), result.getSemester()));
            result.setCounselingStatus(analyzeCounseling(studentId, subjectId));

            // 종합 위험도 판정
            if (aiAvailable) {
                // AI 사용 시도
                try {
                    String aiRisk = geminiService.predictOverallDropoutRisk(result, enrollment);

                    if (aiRisk != null && isValidRiskLevel(aiRisk)) {
                        result.setOverallRisk(aiRisk);
                        log.info("✅ AI 예측 성공: 과목 ID={}, 위험도={}", subjectId, aiRisk);
                    } else {
                        // 첫 실패 시점에 AI 포기하고 모든 과목 규칙 기반으로 전환
                        log.warn("⚠️ AI 예측 실패, 나머지 과목도 규칙 기반 사용: 과목 ID={}", subjectId);
                        aiAvailable = false;
                        fallbackMethod = "AI 예측 실패";

                        // 실패한 과목도 규칙 기반으로
                        result.setOverallRisk(calculateOverallRisk(result));
                    }
                } catch (Exception e) {
                    log.error("❌ AI 예측 에러, 규칙 기반으로 전환: {}", e.getMessage());
                    aiAvailable = false;
                    fallbackMethod = "AI 에러: " + e.getMessage();
                    result.setOverallRisk(calculateOverallRisk(result));
                }
            } else {
                // 이미 AI 실패했으므로 규칙 기반 사용
                result.setOverallRisk(calculateOverallRisk(result));
                log.info("📏 규칙 기반 사용: 과목 ID={}, 위험도={}",
                        subjectId, result.getOverallRisk());
            }

            // AI 코멘트 생성 (RISK/CRITICAL만)
            if (("RISK".equals(result.getOverallRisk()) ||
                    "CRITICAL".equals(result.getOverallRisk())) && aiAvailable) {
                try {
                    String comment = geminiService.generateRiskComment(result, enrollment);
                    result.setAnalysisDetail(comment);
                } catch (Exception e) {
                    log.warn("⚠️ AI 코멘트 생성 실패: {}", e.getMessage());
                    result.setAnalysisDetail(generateFallbackComment(result, enrollment));
                }
            }

            // 저장
            AIAnalysisResult saved = aiAnalysisResultRepository.save(result);
            results.add(saved);

            // Rate Limit 방지 대기 (AI 사용 시)
            if (aiAvailable && i < enrollments.size() - 1) {
                try {
                    Thread.sleep(2000); // 2초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ 대기 중 인터럽트");
                }
            }
        }

        log.info("✅ 학생 전체 과목 분석 완료: 학생 ID={}, 과목 수={}, AI 사용={}, 폴백={}",
                studentId, results.size(), aiAvailable ? "전체" : "없음",
                fallbackMethod != null ? fallbackMethod : "없음");

        // 위험 알림 발송 (RISK/CRITICAL 과목만)
        for (AIAnalysisResult result : results) {
            if ("RISK".equals(result.getOverallRisk()) ||
                    "CRITICAL".equals(result.getOverallRisk())) {
                sendRiskNotifications(result, result.getOverallRisk());
            }
        }

        return results;
    }
}