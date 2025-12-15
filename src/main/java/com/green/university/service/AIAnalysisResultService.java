package com.green.university.service;

import com.green.university.repository.AIAnalysisResultRepository;
import com.green.university.repository.StuSubDetailJpaRepository;
import com.green.university.repository.TuitionJpaRepository;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.AICounseling;
import com.green.university.repository.model.StuSubDetail;
import com.green.university.repository.model.Tuition;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIAnalysisResultService {

    private final AIAnalysisResultRepository aiAnalysisResultRepository;
    private final StuSubDetailJpaRepository stuSubDetailRepository;
    private final TuitionJpaRepository tuitionRepository;
//    private final AICounselingService aiCounselingService;

    private final AICounselingQueryService counselingQueryService;

    @Autowired
    private GeminiService geminiService;

    /**
     * 학생의 분석 결과 조회 - DB에서 조회
     * DB에 없으면 실시간 분석 후 저장
     */
    @Transactional
    public List<AIAnalysisResult> getStudentAnalysisResults(Integer studentId) {
        // 1. DB에서 기존 분석 결과 조회
        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository
                .findByStudentIdOrderByAnalyzedAtDesc(studentId);

        // 2. 학생의 수강 과목 조회
        List<StuSubDetail> enrollments = stuSubDetailRepository.findByStudentIdWithRelations(studentId);

        if (enrollments.isEmpty()) {
            return existingResults;
        }

        // 3. 과목별로 최신 분석 결과가 있는지 확인
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

        // 4. 각 과목별로 분석 결과 확인 및 생성
        for (StuSubDetail enrollment : enrollments) {
            Integer subjectId = enrollment.getSubjectId();

            if (resultMap.containsKey(subjectId)) {
                // DB에 저장된 결과가 있으면 그것을 사용
                results.add(resultMap.get(subjectId));
            } else {
                // DB에 없으면 새로 분석하고 저장
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

        // 각 항목별 분석
        result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
        result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
        result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
        result.setFinalStatus(analyzeFinal(studentId, subjectId));
        result.setTuitionStatus(analyzeTuition(studentId, year, semester));
        result.setCounselingStatus(analyzeCounseling(studentId));

        // 종합 위험도 계산
        result.setOverallRisk(calculateOverallRisk(result));

        // RISK 또는 CRITICAL인 경우 AI 코멘트 생성
        if ("RISK".equals(result.getOverallRisk()) || "CRITICAL".equals(result.getOverallRisk())) {
            try {
                String aiComment = geminiService.generateRiskComment(result, enrollment);
                result.setAnalysisDetail(aiComment);
            } catch (Exception e) {
                System.err.println("AI 코멘트 생성 실패: " + e.getMessage());
                result.setAnalysisDetail(null);
            }
        }

        // DB에 저장
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
     * 전체 학생 분석 결과 조회 - DB에서 조회
     */
    @Transactional(readOnly = true)
    public List<AIAnalysisResult> getAllStudents() {
        // 1. 모든 학생-과목 조합 조회
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject();

        // 2. 기존 AI 분석 결과 조회
        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findAllWithRelations();

        // 3. 기존 분석 결과를 Map으로 변환 (최신 것만)
        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) ->
                                existing.getAnalyzedAt().isAfter(replacement.getAnalyzedAt())
                                        ? existing : replacement
                ));

        // 4. 모든 학생-과목에 대해 결과 생성
        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                // DB에 저장된 분석 결과가 있으면 사용
                allResults.add(resultMap.get(key));
            } else {
                // DB에 없으면 기본값 생성 (아직 분석되지 않음)
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
                defaultResult.setAnalyzedAt(null); // 아직 분석 안됨

                allResults.add(defaultResult);
            }
        }

        return allResults;
    }

    /**
     * AI 분석 실행 - DB에 저장
     */
    @Transactional
    public AIAnalysisResult analyzeStudent(Integer studentId, Integer subjectId,
                                           Integer year, Integer semester) {
        // 기존 분석 결과 확인
        AIAnalysisResult existingResult = getLatestAnalysisResult(studentId, subjectId);

        // 이미 오늘 분석한 결과가 있으면 업데이트, 없으면 새로 생성
        AIAnalysisResult result;
        if (existingResult != null &&
                existingResult.getAnalyzedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate())) {
            result = existingResult;
        } else {
            result = new AIAnalysisResult();
            result.setStudentId(studentId);
            result.setSubjectId(subjectId);
            result.setAnalysisYear(year);
            result.setSemester(semester);
        }

        // 각 항목 분석
        result.setAttendanceStatus(analyzeAttendance(studentId, subjectId));
        result.setHomeworkStatus(analyzeHomework(studentId, subjectId));
        result.setMidtermStatus(analyzeMidterm(studentId, subjectId));
        result.setFinalStatus(analyzeFinal(studentId, subjectId));
        result.setTuitionStatus(analyzeTuition(studentId, year, semester));
        result.setCounselingStatus(analyzeCounseling(studentId));

        // 종합 위험도 계산
        result.setOverallRisk(calculateOverallRisk(result));

        // RISK 또는 CRITICAL인 경우 AI 코멘트 생성
        if ("RISK".equals(result.getOverallRisk()) || "CRITICAL".equals(result.getOverallRisk())) {
            try {
                // StuSubDetail 조회
                StuSubDetail detail = stuSubDetailRepository
                        .findByStudentIdAndSubjectId(studentId, subjectId)
                        .orElse(null);

                String aiComment = geminiService.generateRiskComment(result, detail);
                result.setAnalysisDetail(aiComment);
            } catch (Exception e) {
                System.err.println("AI 코멘트 생성 실패: " + e.getMessage());
                result.setAnalysisDetail(null);
            }
        } else {
            result.setAnalysisDetail(null); // NORMAL, CAUTION은 코멘트 없음
        }

        return aiAnalysisResultRepository.save(result);
    }

    /**
     * 전체 학생-과목에 대한 일괄 AI 분석 실행
     */
    @Transactional
    public int analyzeAllStudentsAndSubjects(Integer year, Integer semester) {
        // 모든 학생-과목 조합 조회
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject();

        int successCount = 0;

        for (StuSubDetail enrollment : allEnrollments) {
            try {
                analyzeStudent(
                        enrollment.getStudentId(),
                        enrollment.getSubjectId(),
                        year != null ? year :
                                (enrollment.getSubject() != null ? enrollment.getSubject().getSubYear() : null),
                        semester != null ? semester :
                                (enrollment.getSubject() != null ? enrollment.getSubject().getSemester() : null)
                );
                successCount++;
            } catch (Exception e) {
                System.err.println("학생 " + enrollment.getStudentId() +
                        ", 과목 " + enrollment.getSubjectId() + " 분석 실패: " + e.getMessage());
            }
        }

        return successCount;
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
    private String analyzeCounseling(Integer studentId) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        List<AICounseling> counselings =
                counselingQueryService.getCompletedCounselingsForAnalysis(studentId);

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
     * 종합 위험도 계산
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
}