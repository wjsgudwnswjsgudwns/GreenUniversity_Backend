package com.green.university.service;

import com.green.university.repository.AIAnalysisResultRepository;
import com.green.university.repository.StuSubDetailJpaRepository;
import com.green.university.repository.TuitionJpaRepository;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.AICounseling;
import com.green.university.repository.model.StuSubDetail;
import com.green.university.repository.model.Tuition;
import lombok.RequiredArgsConstructor;
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
    private final AICounselingService aiCounselingService;

    /**
     * 학생의 분석 결과 조회
     * 실시간으로 데이터를 분석하여 반환 (DB에 저장하지 않음)
     */
    @Transactional(readOnly = true)
    public List<AIAnalysisResult> getStudentAnalysisResults(Integer studentId) {
        // 1. 학생의 수강 과목 조회
        List<StuSubDetail> enrollments = stuSubDetailRepository.findByStudentId(studentId);

        if (enrollments.isEmpty()) {
            return new ArrayList<>();
        }

        // Lazy Loading 강제 초기화
        for (StuSubDetail enrollment : enrollments) {
            if (enrollment.getStudent() != null) {
                enrollment.getStudent().getName();
                if (enrollment.getStudent().getDepartment() != null) {
                    enrollment.getStudent().getDepartment().getName();
                }
            }
            if (enrollment.getSubject() != null) {
                enrollment.getSubject().getName();
                if (enrollment.getSubject().getProfessor() != null) {
                    enrollment.getSubject().getProfessor().getName();
                }
            }
        }

        // 2. 각 과목별로 실시간 분석 수행
        List<AIAnalysisResult> results = new ArrayList<>();

        for (StuSubDetail enrollment : enrollments) {
            AIAnalysisResult result = new AIAnalysisResult();
            result.setStudentId(studentId);
            result.setSubjectId(enrollment.getSubjectId());
            result.setStudent(enrollment.getStudent());
            result.setSubject(enrollment.getSubject());

            // 현재 연도/학기 설정 (실제로는 Subject에서 가져와야 함)
            if (enrollment.getSubject() != null) {
                result.setAnalysisYear(enrollment.getSubject().getSubYear());
                result.setSemester(enrollment.getSubject().getSemester());
            }

            // 실시간 분석 수행
            String attendanceStatus = analyzeAttendance(studentId, enrollment.getSubjectId());
            result.setAttendanceStatus(attendanceStatus);

            String homeworkStatus = analyzeHomework(studentId, enrollment.getSubjectId());
            result.setHomeworkStatus(homeworkStatus);

            String midtermStatus = analyzeMidterm(studentId, enrollment.getSubjectId());
            result.setMidtermStatus(midtermStatus);

            String finalStatus = analyzeFinal(studentId, enrollment.getSubjectId());
            result.setFinalStatus(finalStatus);

            String tuitionStatus = analyzeTuition(studentId,
                    enrollment.getSubject() != null ? enrollment.getSubject().getSubYear() : null,
                    enrollment.getSubject() != null ? enrollment.getSubject().getSemester() : null);
            result.setTuitionStatus(tuitionStatus);

            String counselingStatus = analyzeCounseling(studentId);
            result.setCounselingStatus(counselingStatus);

            // 종합 위험도 계산
            String overallRisk = calculateOverallRisk(result);
            result.setOverallRisk(overallRisk);

            result.setAnalyzedAt(null); // 실시간 분석이므로 null

            results.add(result);
        }

        return results;
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

    @Transactional(readOnly = true)
    public List<AIAnalysisResult> getAllStudents() {
        // 1. 모든 학생-과목 조합 조회 (FETCH JOIN 사용)
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject();

        // 2. 기존 AI 분석 결과 조회 (FETCH JOIN 사용)
        List<AIAnalysisResult> existingResults = aiAnalysisResultRepository.findAllWithRelations();

        // 3. 기존 분석 결과를 Map으로 변환
        Map<String, AIAnalysisResult> resultMap = existingResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getStudentId() + "-" + result.getSubjectId(),
                        result -> result,
                        (existing, replacement) -> existing
                ));

        // 4. 모든 학생-과목에 대해 결과 생성
        List<AIAnalysisResult> allResults = new ArrayList<>();

        for (StuSubDetail enrollment : allEnrollments) {
            String key = enrollment.getStudentId() + "-" + enrollment.getSubjectId();

            if (resultMap.containsKey(key)) {
                allResults.add(resultMap.get(key));
            } else {
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
     * AI 분석 실행 (실제 AI 연동 부분은 추후 구현)
     */
    @Transactional
    public AIAnalysisResult analyzeStudent(Integer studentId, Integer subjectId, Integer year, Integer semester) {
        AIAnalysisResult result = new AIAnalysisResult();
        result.setStudentId(studentId);
        result.setSubjectId(subjectId);
        result.setAnalysisYear(year);
        result.setSemester(semester);

        // 출결 분석
        String attendanceStatus = analyzeAttendance(studentId, subjectId);
        result.setAttendanceStatus(attendanceStatus);

        // 과제 분석
        String homeworkStatus = analyzeHomework(studentId, subjectId);
        result.setHomeworkStatus(homeworkStatus);

        // 중간고사 분석
        String midtermStatus = analyzeMidterm(studentId, subjectId);
        result.setMidtermStatus(midtermStatus);

        // 기말고사 분석
        String finalStatus = analyzeFinal(studentId, subjectId);
        result.setFinalStatus(finalStatus);

        // 등록금 분석
        String tuitionStatus = analyzeTuition(studentId, year, semester);
        result.setTuitionStatus(tuitionStatus);

        // 상담 분석 (나중에 AI 연동)
        String counselingStatus = analyzeCounseling(studentId);
        result.setCounselingStatus(counselingStatus);

        // 종합 위험도 계산
        String overallRisk = calculateOverallRisk(result);
        result.setOverallRisk(overallRisk);

        return aiAnalysisResultRepository.save(result);
    }

    /**
     * 출결 분석 (결석, 지각 - 지각 3회 = 결석 1회)
     * 수정된 기준:
     * - CRITICAL: 3회 이상 결석
     * - RISK: 2회 결석
     * - CAUTION: 1회 결석
     * - NORMAL: 결석 없음 또는 데이터 없음
     */
    private String analyzeAttendance(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        // 데이터가 없으면 정상 (신입생 등)
        if (detail == null) {
            return "NORMAL";
        }

        // absent와 lateness가 모두 null이면 데이터 없음 → 정상
        if (detail.getAbsent() == null && detail.getLateness() == null) {
            return "NORMAL";
        }

        int absent = detail.getAbsent() != null ? detail.getAbsent() : 0;
        int lateness = detail.getLateness() != null ? detail.getLateness() : 0;

        // 지각 3회 = 결석 1회 환산
        double totalAbsent = absent + (lateness / 3.0);

        if (totalAbsent >= 3) {
            return "CRITICAL"; // 3회 이상 결석 (4회 결석 시 F학점 위험)
        } else if (totalAbsent >= 2) {
            return "RISK"; // 2회 결석
        } else if (totalAbsent >= 1) {
            return "CAUTION"; // 1회 결석
        } else {
            return "NORMAL"; // 결석 없음
        }
    }

    /**
     * 과제 분석
     * 데이터가 없으면 NORMAL
     */
    private String analyzeHomework(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        // 데이터가 없거나 homework가 null이면 정상
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
     * 데이터가 없으면 NORMAL
     */
    private String analyzeMidterm(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        // 데이터가 없거나 midExam이 null이면 정상
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
     * 데이터가 없으면 NORMAL
     */
    private String analyzeFinal(Integer studentId, Integer subjectId) {
        StuSubDetail detail = stuSubDetailRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);

        // 데이터가 없거나 finalExam이 null이면 정상
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
     * 수정된 기준:
     * - CAUTION: 미납 (status = false)
     * - NORMAL: 납부 완료 (status = true)
     */
    private String analyzeTuition(Integer studentId, Integer year, Integer semester) {
        Optional<Tuition> tuitionOpt = tuitionRepository
                .findByIdStudentIdAndIdTuiYearAndIdSemester(studentId, year, semester);

        if (tuitionOpt.isEmpty()) {
            // 등록금 정보가 없으면 정상으로 처리 (신입생 등)
            return "NORMAL";
        }

        Tuition tuition = tuitionOpt.get();

        // status가 null이면 미납으로 간주
        if (tuition.getStatus() == null || !tuition.getStatus()) {
            return "CAUTION"; // 미납
        } else {
            return "NORMAL"; // 납부 완료
        }
    }

//    /**
//     * 상담 내용 분석 (AI 연동 필요 - 추후 구현)
//     * 현재는 NORMAL 반환
//     */
//    private String analyzeCounseling(Integer studentId) {
//        // 나중에 AI API를 호출하여 상담 내용 분석 예정
//        return "NORMAL";
//    }

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

        // 종합 판단 로직
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
     * 전체 학생-과목에 대한 일괄 AI 분석 실행
     */
    @Transactional
    public int analyzeAllStudentsAndSubjects(Integer year, Integer semester) {
        // 모든 학생-과목 조합 조회
        List<StuSubDetail> allEnrollments = stuSubDetailRepository.findAllWithStudentAndSubject();

        int successCount = 0;

        for (StuSubDetail enrollment : allEnrollments) {
            try {
                // 이미 분석 결과가 있는지 확인
                AIAnalysisResult existing = getLatestAnalysisResult(
                        enrollment.getStudentId(),
                        enrollment.getSubjectId()
                );

                // 없으면 새로 분석
                if (existing == null) {
                    analyzeStudent(
                            enrollment.getStudentId(),
                            enrollment.getSubjectId(),
                            year,
                            semester
                    );
                    successCount++;
                }
            } catch (Exception e) {
                System.err.println("학생 " + enrollment.getStudentId() +
                        ", 과목 " + enrollment.getSubjectId() + " 분석 실패: " + e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 상담 분석 (횟수 + 개선 추세 반영)
     * - 최근 3개월 상담 횟수
     * - 최근 상담 내용의 위험도 추세
     */
    private String analyzeCounseling(Integer studentId) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        // 최근 3개월 완료된 상담 내역 조회 (최신순)
        List<AICounseling> counselings = aiCounselingService
                .getCompletedCounselingsForAnalysis(studentId);

        // 3개월 이내 상담만 필터링
        List<AICounseling> recentCounselings = counselings.stream()
                .filter(c -> c.getCompletedAt() != null &&
                        c.getCompletedAt().isAfter(threeMonthsAgo))
                .collect(Collectors.toList());

        if (recentCounselings.isEmpty()) {
            return "NORMAL";
        }

        // 1. 상담 횟수 점수 계산
        int frequencyScore = calculateFrequencyScore(recentCounselings.size());

        // 2. 상담 내용 추세 점수 계산
        int trendScore = calculateTrendScore(recentCounselings);

        // 3. 종합 점수 (빈도 30% + 추세 70%)
        int totalScore = (frequencyScore * 30 + trendScore * 70) / 100;

        // 4. 점수에 따른 위험도 판정
        if (totalScore >= 80) {
            return "CRITICAL";
        } else if (totalScore >= 60) {
            return "RISK";
        } else if (totalScore >= 40) {
            return "CAUTION";
        } else {
            return "NORMAL";
        }
    }

    /**
     * 상담 횟수 기반 점수 (0~100)
     * 최근 3개월 기준
     */
    private int calculateFrequencyScore(int counselingCount) {
        if (counselingCount >= 10) {
            return 100; // 월 3회 이상 = 매우 심각 (지속적 문제)
        } else if (counselingCount >= 8) {
            return 85;  // 월 2.5회 이상 = 심각
        } else if (counselingCount >= 6) {
            return 70;  // 월 2회 이상 = 위험
        } else if (counselingCount >= 5) {
            return 55;  // 월 1.5회 이상 = 주의
        } else if (counselingCount >= 4) {
            return 40;  // 월 1회 이상 = 경미한 주의
        } else if (counselingCount >= 3) {
            return 25;  // 3개월에 3회 = 정상 범위
        } else if (counselingCount >= 2) {
            return 15;  // 3개월에 2회 = 정상
        } else {
            return 10;  // 3개월에 1회 = 정상
        }
    }

    /**
     * 상담 내용 추세 기반 점수 (0~100)
     * - 최근 상담의 위험도
     * - 추세 변화 (악화/개선/유지)
     */
    private int calculateTrendScore(List<AICounseling> counselings) {
        if (counselings.isEmpty()) {
            return 0;
        }

        // 최근 3개 상담만 분석 (추세 파악용)
        int analyzeCount = Math.min(3, counselings.size());
        List<AICounseling> recentForTrend = counselings.subList(0, analyzeCount);

        // 각 상담의 위험도 레벨
        List<Integer> riskLevels = new ArrayList<>();
        for (AICounseling counseling : recentForTrend) {
            riskLevels.add(getRiskLevel(counseling.getAiAnalysisResult()));
        }

        // 1. 최근 상담의 위험도 점수 (가중치: 최근일수록 높음)
        double weightedScore = 0;
        double totalWeight = 0;

        for (int i = 0; i < riskLevels.size(); i++) {
            double weight = 1.0 / (i + 1); // 최근: 1.0, 그 다음: 0.5, 그 다음: 0.33
            weightedScore += riskLevels.get(i) * 25 * weight; // 레벨당 25점
            totalWeight += weight;
        }

        int baseScore = (int) (weightedScore / totalWeight);

        // 2. 추세 분석 (개선/악화 여부)
        int trendAdjustment = 0;

        if (riskLevels.size() >= 2) {
            int latest = riskLevels.get(0);
            int previous = riskLevels.get(1);

            if (latest > previous) {
                // 악화 추세: 점수 상승
                trendAdjustment = 15;
            } else if (latest < previous) {
                // 개선 추세: 점수 감소
                trendAdjustment = -15;
            }

            // 3개 이상 있으면 더 정교한 추세 분석
            if (riskLevels.size() >= 3) {
                int beforePrevious = riskLevels.get(2);

                // 지속적 악화: 3회 연속 상승
                if (latest > previous && previous > beforePrevious) {
                    trendAdjustment = 25; // 추가 가중치
                }
                // 지속적 개선: 3회 연속 하락
                else if (latest < previous && previous < beforePrevious) {
                    trendAdjustment = -20; // 개선 추세 인정
                }
                // 불안정: 오르락내리락
                else if (latest == previous && previous != beforePrevious) {
                    trendAdjustment = 10; // 약간의 주의
                }
            }
        }

        // 3. 최근 상담이 CRITICAL이면 무조건 높은 점수
        if ("CRITICAL".equals(counselings.get(0).getAiAnalysisResult())) {
            return Math.max(baseScore + trendAdjustment, 85);
        }

        int finalScore = baseScore + trendAdjustment;
        return Math.max(0, Math.min(100, finalScore)); // 0~100 범위로 제한
    }

    /**
     * 위험도를 숫자 레벨로 변환
     * CRITICAL: 4, RISK: 3, CAUTION: 2, NORMAL: 1
     */
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
}