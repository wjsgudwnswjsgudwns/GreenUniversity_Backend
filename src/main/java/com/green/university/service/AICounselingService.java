package com.green.university.service;

import com.green.university.repository.AICounselingRepository;
import com.green.university.repository.AIAnalysisResultRepository;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.ProfessorJpaRepository;
import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.model.AICounseling;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Professor;
import com.green.university.repository.model.Subject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AICounselingService {

    private final AICounselingRepository aiCounselingRepository;
    private final AIAnalysisResultRepository aiAnalysisResultRepository;
    private final StudentJpaRepository studentRepository;
    private final ProfessorJpaRepository professorRepository;
    private final SubjectJpaRepository subjectRepository;
    private final GeminiService geminiService;

    // ✅ AI 분석 서비스 추가
    private final AIAnalysisResultService aiAnalysisResultService;

    public List<AICounseling> getStudentCounselings(Integer studentId) {
        return aiCounselingRepository.findByStudentIdOrderByScheduledAtDesc(studentId);
    }

    public List<AICounseling> getUpcomingCounselings(Integer studentId) {
        return aiCounselingRepository.findByStudentIdAndIsCompletedFalseOrderByScheduledAtAsc(studentId);
    }

    public List<AICounseling> getProfessorCounselings(Integer professorId) {
        return aiCounselingRepository.findByProfessorIdOrderByScheduledAtDesc(professorId);
    }

    public List<AICounseling> getSubjectCounselings(Integer subjectId) {
        return aiCounselingRepository.findBySubjectIdOrderByScheduledAtDesc(subjectId);
    }

    public List<AICounseling> getCounselingsByProfessorAndStudent(Integer professorId, Integer studentId) {
        return aiCounselingRepository.findByProfessorIdAndStudentId(professorId, studentId);
    }

    @Transactional
    public AICounseling createCounseling(AICounseling counseling) {
        return aiCounselingRepository.save(counseling);
    }

    @Transactional
    public AICounseling createCounselingWithAnalysis(
            Integer studentId,
            Integer professorId,
            Integer subjectId,
            LocalDateTime scheduledAt,
            String counselingContent
    ) {
        System.out.println("=== 상담 기록 + AI 분석 시작 ===");

        // 1. Gemini로 상담 내용 분석
        String riskLevel = geminiService.analyzeCounselingContent(counselingContent);
        System.out.println("Gemini 분석 결과: " + riskLevel);

        // 2. 엔티티 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("학생을 찾을 수 없습니다."));
        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new RuntimeException("교수를 찾을 수 없습니다."));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("과목을 찾을 수 없습니다."));

        // 3. AICounseling 생성
        AICounseling counseling = new AICounseling();
        counseling.setStudent(student);
        counseling.setProfessor(professor);
        counseling.setSubject(subject);
        counseling.setScheduledAt(scheduledAt);
        counseling.setCounselingContent(counselingContent);
        counseling.setIsCompleted(true);
        counseling.setCompletedAt(LocalDateTime.now());
        counseling.setAiAnalysisResult(riskLevel);

        AICounseling saved = aiCounselingRepository.save(counseling);

        // 4. AIAnalysisResult의 counselingStatus 업데이트
        updateCounselingStatus(studentId, subjectId, riskLevel);

        // ✅ 5. 전체 AI 분석 재실행 (실시간 트리거)
        triggerAIAnalysisForCounseling(studentId, subjectId, subject);

        System.out.println("=== 상담 기록 + AI 분석 완료 ===");
        return saved;
    }

    /**
     * ✅ 상담 완료 시 AI 분석 트리거
     */
    private void triggerAIAnalysisForCounseling(Integer studentId, Integer subjectId, Subject subject) {
        try {
            System.out.println("🤖 상담 완료 후 AI 분석 시작: 학생 " + studentId);

            if (subject != null) {
                aiAnalysisResultService.analyzeStudent(
                        studentId,
                        subjectId,
                        subject.getSubYear(),
                        subject.getSemester()
                );
                System.out.println("✅ 상담 완료 후 AI 분석 완료");
            }

        } catch (Exception e) {
            System.err.println("⚠️ AI 분석 실패 (상담 저장은 정상 처리됨): " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCounselingStatus(Integer studentId, Integer subjectId, String riskLevel) {
        try {
            List<AIAnalysisResult> results = aiAnalysisResultRepository
                    .findByStudentIdAndSubjectIdOrderByAnalyzedAtDesc(studentId, subjectId);

            if (!results.isEmpty()) {
                AIAnalysisResult result = results.get(0);
                result.setCounselingStatus(riskLevel);

                String overallRisk = recalculateOverallRisk(result);
                result.setOverallRisk(overallRisk);

                aiAnalysisResultRepository.save(result);
            }
        } catch (Exception e) {
            System.err.println("상담 상태 업데이트 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String recalculateOverallRisk(AIAnalysisResult result) {
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

    // ✅ 상담 내용 작성 및 완료 처리 - AI 분석 트리거 추가
    @Transactional
    public AICounseling completeCounseling(Integer counselingId, String counselingContent) {
        System.out.println("=== 상담 완료 처리 시작 ===");

        AICounseling counseling = aiCounselingRepository.findById(counselingId)
                .orElseThrow(() -> new RuntimeException("상담 일정을 찾을 수 없습니다."));

        counseling.setCounselingContent(counselingContent);
        counseling.setIsCompleted(true);
        counseling.setCompletedAt(LocalDateTime.now());

        // ✅ Gemini AI 분석 추가
        try {
            String riskLevel = geminiService.analyzeCounselingContent(counselingContent);
            counseling.setAiAnalysisResult(riskLevel);
            System.out.println("Gemini 상담 분석 결과: " + riskLevel);
        } catch (Exception e) {
            System.err.println("⚠️ Gemini 분석 실패: " + e.getMessage());
        }

        AICounseling saved = aiCounselingRepository.save(counseling);

        // ✅ AI 전체 분석 트리거
        if (counseling.getSubject() != null) {
            triggerAIAnalysisForCounseling(
                    counseling.getStudentId(),
                    counseling.getSubjectId(),
                    counseling.getSubject()
            );
        }

        System.out.println("=== 상담 완료 처리 완료 ===");
        return saved;
    }

    @Transactional
    public AICounseling updateCounseling(Integer counselingId, LocalDateTime newScheduledAt) {
        AICounseling counseling = aiCounselingRepository.findById(counselingId)
                .orElseThrow(() -> new RuntimeException("상담 일정을 찾을 수 없습니다."));

        counseling.setScheduledAt(newScheduledAt);
        return aiCounselingRepository.save(counseling);
    }

    @Transactional
    public void deleteCounseling(Integer counselingId) {
        aiCounselingRepository.deleteById(counselingId);
    }

    public List<AICounseling> getCompletedCounselingsForAnalysis(Integer studentId) {
        return aiCounselingRepository.findCompletedCounselingsWithContentByStudentId(studentId);
    }
}