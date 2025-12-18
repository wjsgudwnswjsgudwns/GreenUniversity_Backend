package com.green.university.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.university.dto.*;
import com.green.university.repository.*;
import com.green.university.repository.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizedLearningService {

    private final StudentJpaRepository studentRepository;
    private final StuSubJpaRepository stuSubRepository;
    private final StuSubDetailJpaRepository stuSubDetailRepository;
    private final SubjectJpaRepository subjectRepository;
    private final GradeJpaRepository gradeRepository;
    private final PersonalizedLearningJpaRepository analysisRepository;
    private final RecommendedSubjectJpaRepository recommendedSubjectRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 학생 맞춤형 학습 지원 데이터 조회 (DB에서 가져오거나 새로 생성)
     */
    @Transactional
    public PersonalizedLearningDTO getPersonalizedLearning(Integer studentId) {
        // 1. 최신 분석 결과가 있는지 확인
        Optional<PersonalizedLearningAnalysis> latestAnalysis =
                analysisRepository.findByStudentIdAndIsLatestTrue(studentId);

        if (latestAnalysis.isPresent()) {
            log.info("DB에서 기존 분석 결과 조회 - 학생 ID: {}", studentId);
            return convertToDTO(latestAnalysis.get());
        }

        // 2. 없으면 새로 생성
        log.info("새로운 분석 결과 생성 - 학생 ID: {}", studentId);
        return generateAndSaveAnalysis(studentId);
    }

    /**
     * 강제로 새로운 분석 생성 (기존 데이터 무시)
     */
    @Transactional
    public PersonalizedLearningDTO regenerateAnalysis(Integer studentId) {
        log.info("분석 결과 재생성 - 학생 ID: {}", studentId);
        return generateAndSaveAnalysis(studentId);
    }

    /**
     * 특정 학기의 분석 결과 조회
     */
    @Transactional(readOnly = true)
    public PersonalizedLearningDTO getAnalysisByPeriod(Integer studentId, Integer year, Integer semester) {
        PersonalizedLearningAnalysis analysis = analysisRepository
                .findByStudentIdAndAnalysisYearAndAnalysisSemester(studentId, year, semester)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("해당 학기의 분석 결과가 없습니다: %d년 %d학기", year, semester)));

        return convertToDTO(analysis);
    }

    /**
     * 학생의 전체 분석 이력 조회
     */
    @Transactional(readOnly = true)
    public List<PersonalizedLearningDTO> getAnalysisHistory(Integer studentId) {
        List<PersonalizedLearningAnalysis> history =
                analysisRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        return history.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 분석 결과 생성 및 저장
     */
    @Transactional
    private PersonalizedLearningDTO generateAndSaveAnalysis(Integer studentId) {
        // 1. 학생 정보 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다: " + studentId));

        // 2. 수강 이력 조회
        List<StuSub> enrolledSubjects = stuSubRepository.findEnrolledByStudentId(studentId);
        List<StuSubDetail> detailList = stuSubDetailRepository.findByStudentIdWithRelations(studentId);

        // 3. 학습 이력 분석
        LearningHistoryAnalysis historyAnalysis = analyzeLearningHistory(student, enrolledSubjects, detailList);

        // 4. 추천 과목 생성
        List<RecommendedSubject> recommendedMajors = recommendMajorSubjects(student, enrolledSubjects, historyAnalysis);
        List<RecommendedSubject> recommendedElectives = recommendElectiveSubjects(student, enrolledSubjects, historyAnalysis);

        // 5. 학습 방향 제시
        LearningDirection learningDirection = generateLearningDirection(student, historyAnalysis, detailList);

        // 6. 졸업 요건 분석
        GraduationRequirement graduationReq = analyzeGraduationRequirement(student, historyAnalysis);

        // 7. AI 종합 분석 코멘트
        String aiComment = generateAIAnalysisComment(student, historyAnalysis, learningDirection);

        // 8. DB에 저장
        PersonalizedLearningAnalysis savedAnalysis = saveAnalysisToDatabase(
                student, historyAnalysis, learningDirection, graduationReq, aiComment, recommendedElectives);

        // 9. DTO로 변환하여 반환
        return convertToDTO(savedAnalysis);
    }

    /**
     * 분석 결과를 DB에 저장
     */
    @Transactional
    private PersonalizedLearningAnalysis saveAnalysisToDatabase(
            Student student,
            LearningHistoryAnalysis historyAnalysis,
            LearningDirection learningDirection,
            GraduationRequirement graduationReq,
            String aiComment,
            List<RecommendedSubject> recommendedElectives) {  // 파라미터 하나로 줄임

        try {
            log.info("=== 분석 결과 저장 시작 ===");

            // 1. 기존 최신 분석 비활성화
            analysisRepository.updatePreviousLatestToFalse(student.getId());

            // 2. 새로운 분석 엔티티 생성
            PersonalizedLearningAnalysis analysis = PersonalizedLearningAnalysis.builder()
                    .studentId(student.getId())
                    .overallGPA(historyAnalysis.getOverallGPA())
                    .majorGPA(historyAnalysis.getMajorGPA())
                    .electiveGPA(historyAnalysis.getElectiveGPA())
                    .recentGPA(historyAnalysis.getRecentGPA())
                    .totalCredits(historyAnalysis.getTotalCredits())
                    .majorCredits(historyAnalysis.getMajorCredits())
                    .electiveCredits(historyAnalysis.getElectiveCredits())
                    .gradeTrend(historyAnalysis.getGradeTrend())
                    // .attendanceRate(historyAnalysis.getAttendanceRate())  // 제거
                    // .homeworkRate(historyAnalysis.getHomeworkRate())  // 제거
                    .strongAreas(toJson(historyAnalysis.getStrongAreas()))
                    .weakAreas(toJson(historyAnalysis.getWeakAreas()))
                    .strengths(learningDirection.getStrengths())
                    .weaknesses(learningDirection.getWeaknesses())
                    .improvementSuggestions(toJson(learningDirection.getImprovementSuggestions()))
                    .learningStrategies(toJson(learningDirection.getLearningStrategies()))
                    .cautions(toJson(learningDirection.getCautions()))
                    .recommendedPattern(learningDirection.getRecommendedPattern())
                    .totalRequiredCredits(graduationReq.getTotalRequiredCredits())
                    .remainingCredits(graduationReq.getRemainingCredits())
                    .majorRequiredCredits(graduationReq.getMajorRequiredCredits())
                    .majorRemainingCredits(graduationReq.getMajorRemainingCredits())
                    .electiveRequiredCredits(graduationReq.getElectiveRequiredCredits())
                    .electiveRemainingCredits(graduationReq.getElectiveRemainingCredits())
                    .canGraduate(graduationReq.getCanGraduate())
                    .semestersToGraduation(graduationReq.getSemestersToGraduation())
                    .recommendedCreditsPerSemester(graduationReq.getRecommendedCreditsPerSemester())
                    .aiAnalysisComment(aiComment)
                    .analysisYear(LocalDate.now().getYear())
                    .analysisSemester(getCurrentSemester())
                    .isLatest(true)
                    .build();

            PersonalizedLearningAnalysis savedAnalysis = analysisRepository.save(analysis);

            // 3. 추천 교양 과목만 저장
            saveRecommendedSubjects(savedAnalysis.getId(), recommendedElectives, "ELECTIVE");

            log.info("=== 분석 결과 저장 완료 ===");
            return savedAnalysis;

        } catch (Exception e) {
            log.error("분석 결과 저장 실패", e);
            throw new RuntimeException("분석 결과 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 추천 과목 저장
     */
    private void saveRecommendedSubjects(Integer analysisId, List<RecommendedSubject> subjects, String type) {
        log.info("추천 과목 저장 - analysisId: {}, type: {}, 개수: {}", analysisId, type, subjects.size());

        int order = 1;
        for (RecommendedSubject subject : subjects) {
            log.info("과목 저장 - subjectId: {}, name: {}, score: {}",
                    subject.getSubjectId(), subject.getSubjectName(), subject.getRecommendScore());

            RecommendedSubjectEntity entity = RecommendedSubjectEntity.builder()
                    .analysisId(analysisId)
                    .subjectId(subject.getSubjectId())
                    .recommendationType(type)
                    .recommendScore(subject.getRecommendScore())
                    .recommendReason(subject.getRecommendReason())
                    .displayOrder(order++)
                    .build();

            RecommendedSubjectEntity saved = recommendedSubjectRepository.save(entity);
            log.info("저장 완료 - ID: {}", saved.getId());
        }
    }

    /**
     * Entity를 DTO로 변환
     */
    private PersonalizedLearningDTO convertToDTO(PersonalizedLearningAnalysis analysis) {
        Student student = analysis.getStudent();
        if (student == null) {
            student = studentRepository.findById(analysis.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다"));
        }

        // 학습 이력 분석
        LearningHistoryAnalysis historyAnalysis = LearningHistoryAnalysis.builder()
                .overallGPA(analysis.getOverallGPA())
                .majorGPA(analysis.getMajorGPA())
                .electiveGPA(analysis.getElectiveGPA())
                .recentGPA(analysis.getRecentGPA())
                .totalCredits(analysis.getTotalCredits())
                .majorCredits(analysis.getMajorCredits())
                .electiveCredits(analysis.getElectiveCredits())
                .gradeTrend(analysis.getGradeTrend())
                .attendanceRate(analysis.getAttendanceRate())
                .homeworkRate(analysis.getHomeworkRate())
                .strongAreas(fromJson(analysis.getStrongAreas(), new TypeReference<List<String>>() {}))
                .weakAreas(fromJson(analysis.getWeakAreas(), new TypeReference<List<String>>() {}))
                .build();

        // 학습 방향
        LearningDirection learningDirection = LearningDirection.builder()
                .strengths(analysis.getStrengths())
                .weaknesses(analysis.getWeaknesses())
                .improvementSuggestions(fromJson(analysis.getImprovementSuggestions(), new TypeReference<List<String>>() {}))
                .learningStrategies(fromJson(analysis.getLearningStrategies(), new TypeReference<List<String>>() {}))
                .cautions(fromJson(analysis.getCautions(), new TypeReference<List<String>>() {}))
                .recommendedPattern(analysis.getRecommendedPattern())
                .build();

        // 졸업 요건
        GraduationRequirement graduationReq = GraduationRequirement.builder()
                .totalRequiredCredits(analysis.getTotalRequiredCredits())
                .currentCredits(analysis.getTotalCredits())
                .remainingCredits(analysis.getRemainingCredits())
                .majorRequiredCredits(analysis.getMajorRequiredCredits())
                .majorCompletedCredits(analysis.getMajorCredits())
                .majorRemainingCredits(analysis.getMajorRemainingCredits())
                .electiveRequiredCredits(analysis.getElectiveRequiredCredits())
                .electiveCompletedCredits(analysis.getElectiveCredits())
                .electiveRemainingCredits(analysis.getElectiveRemainingCredits())
                .canGraduate(analysis.getCanGraduate())
                .semestersToGraduation(analysis.getSemestersToGraduation())
                .recommendedCreditsPerSemester(analysis.getRecommendedCreditsPerSemester())
                .build();

        List<RecommendedSubject> recommendedElectives = getRecommendedSubjectsSafe(analysis.getId(), "ELECTIVE");

        return PersonalizedLearningDTO.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .departmentName(student.getDepartment().getName())
                .currentGrade(student.getGrade())
                .currentSemester(student.getSemester())
                .learningHistory(historyAnalysis)
                .recommendedMajors(new ArrayList<>())  // 빈 리스트
                .recommendedElectives(recommendedElectives)  // 교양만
                .learningDirection(learningDirection)
                .graduationRequirement(graduationReq)
                .aiAnalysisComment(analysis.getAiAnalysisComment())
                .analysisDate(analysis.getCreatedAt())
                .build();
    }

    private List<RecommendedSubject> getRecommendedSubjectsSafe(Integer analysisId, String type) {
        log.info("=== 추천 과목 안전 조회 시작 ===");
        log.info("analysisId: {}, type: {}", analysisId, type);

        try {
            // 기본 조회 (JOIN FETCH 없이)
            List<RecommendedSubjectEntity> entities =
                    recommendedSubjectRepository.findByAnalysisIdAndRecommendationTypeOrderByDisplayOrder(
                            analysisId, type);

            log.info("조회된 엔티티 수: {}", entities.size());

            return entities.stream()
                    .map(e -> {
                        try {
                            // 과목을 직접 조회
                            Subject subject = subjectRepository.findById(e.getSubjectId())
                                    .orElse(null);

                            if (subject == null) {
                                log.warn("Subject not found for ID: {}", e.getSubjectId());
                                return null;
                            }

                            return RecommendedSubject.builder()
                                    .subjectId(subject.getId())
                                    .subjectName(subject.getName())
                                    .professorName(subject.getProfessor() != null ?
                                            subject.getProfessor().getName() : "미정")
                                    .credits(subject.getGrades())
                                    .subjectType(subject.getType())
                                    .recommendScore(e.getRecommendScore())
                                    .recommendReason(e.getRecommendReason())
                                    .capacity(subject.getCapacity())
                                    .currentStudents(subject.getNumOfStudent())
                                    .build();
                        } catch (Exception ex) {
                            log.error("과목 변환 중 에러 - subjectId: {}", e.getSubjectId(), ex);
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("추천 과목 조회 실패 - analysisId: {}, type: {}", analysisId, type, e);
            return new ArrayList<>();
        }
    }

    /**
     * 추천 과목 조회
     */
    private List<RecommendedSubject> getRecommendedSubjects(Integer analysisId, String type) {
        return getRecommendedSubjectsSafe(analysisId, type);
    }

    private List<String> analyzeWeakAreas(List<StuSub> subjects) {
        Map<String, Double> typeGPAs = subjects.stream()
                .filter(s -> s.getGrade() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getSubject().getType(),
                        Collectors.averagingDouble(s -> {
                            Optional<Grade> grade = gradeRepository.findByGrade(s.getGrade());
                            return grade.map(g -> g.getGradeValue().doubleValue()).orElse(0.0);
                        })
                ));
        return typeGPAs.entrySet().stream()
                .filter(e -> e.getValue() < 3.0)
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ========== 기존 분석 메서드들 ==========

    private LearningHistoryAnalysis analyzeLearningHistory(Student student,
                                                           List<StuSub> enrolledSubjects,
                                                           List<StuSubDetail> detailList) {
        Double overallGPA = calculateGPA(enrolledSubjects);
        Double majorGPA = calculateMajorGPA(enrolledSubjects);
        Double electiveGPA = calculateElectiveGPA(enrolledSubjects);
        Double recentGPA = calculateRecentGPA(enrolledSubjects);

        int totalCredits = calculateTotalCredits(enrolledSubjects);
        int majorCredits = calculateMajorCredits(enrolledSubjects);
        int electiveCredits = calculateElectiveCredits(enrolledSubjects);

        String gradeTrend = analyzeGradeTrend(enrolledSubjects);
        Double attendanceRate = calculateAttendanceRate(detailList);
        Double homeworkRate = calculateHomeworkRate(detailList);

        List<String> strongAreas = analyzeStrongAreas(enrolledSubjects);
        List<String> weakAreas = analyzeWeakAreas(enrolledSubjects);

        return LearningHistoryAnalysis.builder()
                .overallGPA(overallGPA)
                .majorGPA(majorGPA)
                .electiveGPA(electiveGPA)
                .recentGPA(recentGPA)
                .totalCredits(totalCredits)
                .majorCredits(majorCredits)
                .electiveCredits(electiveCredits)
                .gradeTrend(gradeTrend)
                .attendanceRate(attendanceRate)
                .homeworkRate(homeworkRate)
                .strongAreas(strongAreas)
                .weakAreas(weakAreas)
                .build();
    }

    private List<RecommendedSubject> recommendMajorSubjects(Student student,
                                                            List<StuSub> enrolledSubjects,
                                                            LearningHistoryAnalysis analysis) {
        Set<Integer> completedSubjectIds = enrolledSubjects.stream()
                .map(StuSub::getSubjectId)
                .collect(Collectors.toSet());

        List<Subject> allSubjects = subjectRepository.findAll();

        return allSubjects.stream()
                .filter(subject -> subject.getDepartment().getId().equals(student.getDeptId()))
                .filter(subject -> !completedSubjectIds.contains(subject.getId()))
                .filter(subject -> "전공".equals(subject.getType()))  // ← 이렇게 수정
                .map(subject -> {
                    double score = calculateRecommendScore(subject, student, analysis);
                    String reason = generateRecommendReason(subject, student, analysis);
                    return createRecommendedSubject(subject, score, reason);
                })
                .sorted(Comparator.comparing(RecommendedSubject::getRecommendScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<RecommendedSubject> recommendElectiveSubjects(Student student,
                                                               List<StuSub> enrolledSubjects,
                                                               LearningHistoryAnalysis analysis) {
        Set<Integer> completedSubjectIds = enrolledSubjects.stream()
                .map(StuSub::getSubjectId)
                .collect(Collectors.toSet());

        List<Subject> allSubjects = subjectRepository.findAll();

        return allSubjects.stream()
                .filter(subject -> !completedSubjectIds.contains(subject.getId()))
                .filter(subject -> "교양".equals(subject.getType()))  // ← 이렇게 수정
                .map(subject -> {
                    double score = calculateRecommendScore(subject, student, analysis);
                    String reason = generateRecommendReason(subject, student, analysis);
                    return createRecommendedSubject(subject, score, reason);
                })
                .sorted(Comparator.comparing(RecommendedSubject::getRecommendScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private LearningDirection generateLearningDirection(Student student,
                                                        LearningHistoryAnalysis analysis,
                                                        List<StuSubDetail> detailList) {
        String strengths = generateStrengthsAnalysis(analysis);
        String weaknesses = generateWeaknessesAnalysis(analysis);
        List<String> improvements = generateImprovementSuggestions(analysis, detailList);
        List<String> strategies = generateLearningStrategies(analysis);
        List<String> cautions = generateCautions(analysis);
        String pattern = recommendLearningPattern(analysis);

        return LearningDirection.builder()
                .strengths(strengths)
                .weaknesses(weaknesses)
                .improvementSuggestions(improvements)
                .learningStrategies(strategies)
                .cautions(cautions)
                .recommendedPattern(pattern)
                .build();
    }

    private GraduationRequirement analyzeGraduationRequirement(Student student,
                                                               LearningHistoryAnalysis analysis) {
        int totalRequired = 130;
        int majorRequired = 65;
        int electiveRequired = 35;

        int remainingTotal = totalRequired - analysis.getTotalCredits();
        int remainingMajor = majorRequired - analysis.getMajorCredits();
        int remainingElective = electiveRequired - analysis.getElectiveCredits();

        boolean canGraduate = remainingTotal <= 0 && remainingMajor <= 0 && remainingElective <= 0;

        int semestersLeft = canGraduate ? 0 : (int) Math.ceil(remainingTotal / 18.0);
        int recommendedPerSemester = semestersLeft > 0 ? (int) Math.ceil((double) remainingTotal / semestersLeft) : 0;

        return GraduationRequirement.builder()
                .totalRequiredCredits(totalRequired)
                .currentCredits(analysis.getTotalCredits())
                .remainingCredits(Math.max(0, remainingTotal))
                .majorRequiredCredits(majorRequired)
                .majorCompletedCredits(analysis.getMajorCredits())
                .majorRemainingCredits(Math.max(0, remainingMajor))
                .electiveRequiredCredits(electiveRequired)
                .electiveCompletedCredits(analysis.getElectiveCredits())
                .electiveRemainingCredits(Math.max(0, remainingElective))
                .canGraduate(canGraduate)
                .semestersToGraduation(semestersLeft)
                .recommendedCreditsPerSemester(recommendedPerSemester)
                .build();
    }

    private String generateAIAnalysisComment(Student student,
                                             LearningHistoryAnalysis analysis,
                                             LearningDirection direction) {
        try {
            return generateRuleBasedComment(analysis);
        } catch (Exception e) {
            log.error("AI 분석 코멘트 생성 실패", e);
            return generateRuleBasedComment(analysis);
        }
    }

    // ========== 계산 헬퍼 메서드들 ==========

    private Double calculateGPA(List<StuSub> subjects) {
        if (subjects.isEmpty()) return 0.0;
        double totalPoints = 0.0;
        int totalCredits = 0;
        for (StuSub stuSub : subjects) {
            if (stuSub.getGrade() != null && stuSub.getCompleteGrade() != null) {
                Optional<Grade> gradeOpt = gradeRepository.findByGrade(stuSub.getGrade());
                if (gradeOpt.isPresent()) {
                    totalPoints += gradeOpt.get().getGradeValue().doubleValue() * stuSub.getSubject().getGrades();
                    totalCredits += stuSub.getSubject().getGrades();
                }
            }
        }
        return totalCredits > 0 ? Math.round((totalPoints / totalCredits) * 100.0) / 100.0 : 0.0;
    }

    private Double calculateMajorGPA(List<StuSub> subjects) {
        List<StuSub> majorSubjects = subjects.stream()
                .filter(s -> "전공".equals(s.getSubject().getType()))
                .collect(Collectors.toList());
        return calculateGPA(majorSubjects);
    }

    private Double calculateElectiveGPA(List<StuSub> subjects) {
        List<StuSub> electiveSubjects = subjects.stream()
                .filter(s -> "교양".equals(s.getSubject().getType()))
                .collect(Collectors.toList());
        return calculateGPA(electiveSubjects);
    }

    private Double calculateRecentGPA(List<StuSub> subjects) {
        List<StuSub> sortedSubjects = subjects.stream()
                .sorted(Comparator.comparing((StuSub s) -> s.getSubject().getSubYear())
                        .thenComparing(s -> s.getSubject().getSemester())
                        .reversed())
                .limit(20)
                .collect(Collectors.toList());
        return calculateGPA(sortedSubjects);
    }

    private int calculateTotalCredits(List<StuSub> subjects) {
        return subjects.stream()
                .filter(s -> s.getGrade() != null && !s.getGrade().equals("F"))
                .mapToInt(s -> s.getSubject().getGrades())
                .sum();
    }

    private int calculateMajorCredits(List<StuSub> subjects) {
        return subjects.stream()
                .filter(s -> s.getGrade() != null && !s.getGrade().equals("F"))
                .filter(s -> "전공".equals(s.getSubject().getType()))  // ← 이렇게 수정
                .mapToInt(s -> s.getSubject().getGrades())
                .sum();
    }
    private int calculateElectiveCredits(List<StuSub> subjects) {
        return subjects.stream()
                .filter(s -> s.getGrade() != null && !s.getGrade().equals("F"))
                .filter(s -> "교양".equals(s.getSubject().getType()))  // ← 이렇게 수정
                .mapToInt(s -> s.getSubject().getGrades())
                .sum();
    }

    private String analyzeGradeTrend(List<StuSub> subjects) {
        if (subjects.size() < 2) return "데이터 부족";
        List<StuSub> sorted = subjects.stream()
                .sorted(Comparator.comparing((StuSub s) -> s.getSubject().getSubYear())
                        .thenComparing(s -> s.getSubject().getSemester()))
                .collect(Collectors.toList());
        int half = sorted.size() / 2;
        List<StuSub> firstHalf = sorted.subList(0, half);
        List<StuSub> secondHalf = sorted.subList(half, sorted.size());
        double firstGPA = calculateGPA(firstHalf);
        double secondGPA = calculateGPA(secondHalf);
        if (secondGPA > firstGPA + 0.3) return "상승 추세";
        if (secondGPA < firstGPA - 0.3) return "하락 추세";
        return "유지";
    }

    private Double calculateAttendanceRate(List<StuSubDetail> details) {
        if (details.isEmpty()) return 100.0;
        int totalAbsent = details.stream().mapToInt(d -> d.getAbsent() != null ? d.getAbsent() : 0).sum();
        int totalLateness = details.stream().mapToInt(d -> d.getLateness() != null ? d.getLateness() : 0).sum();
        int totalClasses = details.size() * 15;
        int missedClasses = totalAbsent + (totalLateness / 3);
        return Math.max(0, Math.round((1 - (double) missedClasses / totalClasses) * 100.0 * 100.0) / 100.0);
    }

    private Double calculateHomeworkRate(List<StuSubDetail> details) {
        if (details.isEmpty()) return 0.0;
        double totalScore = details.stream().mapToInt(d -> d.getHomework() != null ? d.getHomework() : 0).average().orElse(0.0);
        return Math.round(totalScore * 100.0) / 100.0;
    }

    private List<String> analyzeStrongAreas(List<StuSub> subjects) {
        Map<String, Double> typeGPAs = subjects.stream()
                .filter(s -> s.getGrade() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getSubject().getType(),
                        Collectors.averagingDouble(s -> {
                            Optional<Grade> grade = gradeRepository.findByGrade(s.getGrade());
                            return grade.map(g -> g.getGradeValue().doubleValue()).orElse(0.0);
                        })
                ));
        return typeGPAs.entrySet().stream()
                .filter(e -> e.getValue() >= 3.5)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private double calculateRecommendScore(Subject subject, Student student, LearningHistoryAnalysis analysis) {
        double score = 50.0;
        if (subject.getGrades() != null && student.getGrade() != null) {
            int gradeDiff = Math.abs(student.getGrade() - subject.getGrades());
            score += Math.max(0, 20 - (gradeDiff * 5));
        }
        if (subject.getCapacity() != null && subject.getNumOfStudent() != null) {
            double fillRate = (double) subject.getNumOfStudent() / subject.getCapacity();
            if (fillRate < 0.8) score += 15;
            else if (fillRate < 0.9) score += 10;
        }
        if (analysis.getStrongAreas().contains(subject.getType())) {
            score += 15;
        }
        return Math.min(100.0, Math.round(score * 10.0) / 10.0);
    }

    private String generateRecommendReason(Subject subject, Student student, LearningHistoryAnalysis analysis) {
        List<String> reasons = new ArrayList<>();
        if (analysis.getStrongAreas().contains(subject.getType())) {
            reasons.add("강점 분야 과목");
        }
        if (subject.getCapacity() != null && subject.getNumOfStudent() != null) {
            double fillRate = (double) subject.getNumOfStudent() / subject.getCapacity();
            if (fillRate < 0.7) {
                reasons.add("여유로운 정원");
            }
        }
        if (student.getGrade() != null && subject.getGrades() != null &&
                student.getGrade().equals(subject.getGrades())) {
            reasons.add("학년 적합");
        }
        return reasons.isEmpty() ? "추천 과목" : String.join(", ", reasons);
    }

    private RecommendedSubject createRecommendedSubject(Subject subject, double score, String reason) {
        return RecommendedSubject.builder()
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .professorName(subject.getProfessor().getName())
                .credits(subject.getGrades())
                .subjectType(subject.getType())
                .recommendScore(score)
                .recommendReason(reason)
                .capacity(subject.getCapacity())
                .currentStudents(subject.getNumOfStudent())
                .build();
    }

    private String generateStrengthsAnalysis(LearningHistoryAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        if (analysis.getOverallGPA() >= 3.5) {
            sb.append("전체 평점 ").append(analysis.getOverallGPA()).append("으로 우수한 학업 성취도를 보이고 있습니다. ");
        }
        if (!analysis.getStrongAreas().isEmpty()) {
            sb.append(String.join(", ", analysis.getStrongAreas())).append(" 분야에서 특히 강점을 보입니다.");
        }
        return sb.toString();
    }

    private String generateWeaknessesAnalysis(LearningHistoryAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        if (analysis.getAttendanceRate() < 80) {
            sb.append("출석률이 ").append(analysis.getAttendanceRate()).append("%로 개선이 필요합니다. ");
        }
        if (!analysis.getWeakAreas().isEmpty()) {
            sb.append(String.join(", ", analysis.getWeakAreas())).append(" 분야에 보완이 필요합니다.");
        }
        return sb.toString();
    }

    private List<String> generateImprovementSuggestions(LearningHistoryAnalysis analysis, List<StuSubDetail> details) {
        List<String> suggestions = new ArrayList<>();
        if (analysis.getAttendanceRate() < 90) {
            suggestions.add("출석률 향상: 수업 일정 관리 앱 활용 권장");
        }
        if (analysis.getHomeworkRate() < 70) {
            suggestions.add("과제 제출률 향상: 과제 알림 설정 및 계획적인 시간 관리");
        }
        if ("하락 추세".equals(analysis.getGradeTrend())) {
            suggestions.add("성적 회복: 학습 컨설팅 및 멘토링 프로그램 참여 권장");
        }
        if (!analysis.getWeakAreas().isEmpty()) {
            suggestions.add("약점 보완: " + analysis.getWeakAreas().get(0) + " 관련 기초 과목 재수강 고려");
        }
        return suggestions;
    }

    private List<String> generateLearningStrategies(LearningHistoryAnalysis analysis) {
        List<String> strategies = new ArrayList<>();
        if (analysis.getOverallGPA() >= 3.5) {
            strategies.add("현재 수준 유지하며 전공 심화 과목 도전");
        } else if (analysis.getOverallGPA() >= 3.0) {
            strategies.add("기본 개념 탄탄히 하며 점진적 난이도 상승");
        } else {
            strategies.add("기초 과목 집중 이수 및 학습 방법 개선");
        }
        if (!analysis.getStrongAreas().isEmpty()) {
            strategies.add(analysis.getStrongAreas().get(0) + " 강점 활용한 학습 계획 수립");
        }
        strategies.add("학기당 적정 학점(15-18학점) 이수로 학업 부담 관리");
        return strategies;
    }

    private List<String> generateCautions(LearningHistoryAnalysis analysis) {
        List<String> cautions = new ArrayList<>();
        if (analysis.getRecentGPA() < analysis.getOverallGPA() - 0.5) {
            cautions.add("최근 성적 하락 주의: 학습 패턴 점검 필요");
        }
        if (analysis.getAttendanceRate() < 80) {
            cautions.add("출석 관리 주의: F학점 위험");
        }
        if (analysis.getTotalCredits() < (analysis.getTotalCredits() / 4) * 15) {
            cautions.add("이수 학점 부족: 졸업 시기 지연 가능성");
        }
        return cautions;
    }

    private String recommendLearningPattern(LearningHistoryAnalysis analysis) {
        if (analysis.getOverallGPA() >= 3.7 && analysis.getAttendanceRate() >= 95) {
            return "심화 학습형: 전공 심화 및 복수전공/부전공 추천";
        } else if (analysis.getOverallGPA() >= 3.0 && analysis.getAttendanceRate() >= 85) {
            return "안정 성장형: 균형잡힌 전공/교양 이수";
        } else {
            return "기초 강화형: 기본 과목 충실히 이수 후 확장";
        }
    }

    private String generateRuleBasedComment(LearningHistoryAnalysis analysis) {
        StringBuilder comment = new StringBuilder();
        if (analysis.getOverallGPA() >= 3.5) {
            comment.append("현재 평점 ").append(analysis.getOverallGPA()).append("으로 매우 우수한 학업 성취를 보이고 있습니다. ");
        } else if (analysis.getOverallGPA() >= 3.0) {
            comment.append("평점 ").append(analysis.getOverallGPA()).append("으로 양호한 수준을 유지하고 있습니다. ");
        } else {
            comment.append("현재 평점 ").append(analysis.getOverallGPA()).append("으로 개선의 여지가 있습니다. ");
        }
        if ("상승 추세".equals(analysis.getGradeTrend())) {
            comment.append("성적이 꾸준히 상승하고 있어 긍정적인 학습 패턴을 보이고 있습니다. ");
        } else if ("하락 추세".equals(analysis.getGradeTrend())) {
            comment.append("최근 성적 하락이 관찰되어 학습 방법 점검이 필요합니다. ");
        }
        if (analysis.getAttendanceRate() >= 90) {
            comment.append("출석률 ").append(analysis.getAttendanceRate()).append("%로 성실한 학습 태도를 유지하고 있습니다. ");
        } else if (analysis.getAttendanceRate() < 80) {
            comment.append("출석률 ").append(analysis.getAttendanceRate()).append("%로 개선이 시급합니다. ");
        }
        if (!analysis.getStrongAreas().isEmpty()) {
            comment.append(analysis.getStrongAreas().get(0)).append(" 분야의 강점을 살려 관련 전공 심화 과목 이수를 권장합니다. ");
        }
        comment.append("지속적인 노력과 계획적인 학습으로 더 나은 성과를 기대합니다.");
        return comment.toString();
    }

    // ========== JSON 변환 유틸리티 ==========

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패", e);
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("JSON 파싱 실패", e);
            return null;
        }
    }

    private int getCurrentSemester() {
        int month = LocalDate.now().getMonthValue();
        return (month >= 3 && month <= 8) ? 1 : 2;
    }
}