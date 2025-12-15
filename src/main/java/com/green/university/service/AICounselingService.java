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
    private final NotificationService notificationService;

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
                String previousRisk = result.getOverallRisk();
                result.setCounselingStatus(riskLevel);

                String overallRisk = recalculateOverallRisk(result);
                result.setOverallRisk(overallRisk);

                AIAnalysisResult saved = aiAnalysisResultRepository.save(result);

                // 위험도가 RISK 또는 CRITICAL로 변경된 경우 알림 발송
                if ((overallRisk.equals("RISK") || overallRisk.equals("CRITICAL")) &&
                    (previousRisk == null || !previousRisk.equals(overallRisk))) {
                    sendRiskNotifications(saved, overallRisk);
                }
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

    /**
     * 위험도가 RISK 또는 CRITICAL일 때 알림 발송
     */
    private void sendRiskNotifications(AIAnalysisResult result, String riskLevel) {
        try {
            Integer studentId = result.getStudentId();
            Integer subjectId = result.getSubjectId();

            if (studentId == null || subjectId == null) {
                System.err.println("학생 ID 또는 과목 ID가 null입니다. 알림 발송 건너뜀.");
                return;
            }

            // 학생 정보 조회
            Student student = studentRepository.findById(studentId)
                    .orElse(null);
            if (student == null) {
                System.err.println("학생을 찾을 수 없습니다. ID: " + studentId);
                return;
            }

            // 과목 정보 조회 (교수 정보 포함)
            Subject subject = subjectRepository.findById(subjectId)
                    .orElse(null);
            if (subject == null) {
                System.err.println("과목을 찾을 수 없습니다. ID: " + subjectId);
                return;
            }

            if (subject.getProfessor() == null) {
                System.err.println("과목에 교수 정보가 없습니다. 과목 ID: " + subjectId);
                return;
            }

            String studentName = student.getName();
            String subjectName = subject.getName();
            Integer professorId = subject.getProfessor().getId();
            String professorName = subject.getProfessor().getName();

            String riskLabel = riskLevel.equals("CRITICAL") ? "심각" : "위험";

            // 학생에게 알림
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

            // 교수에게 알림
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

            System.out.println("위험 알림 발송 완료: 학생=" + studentName + ", 과목=" + subjectName + ", 위험도=" + riskLevel);
        } catch (Exception e) {
            System.err.println("위험 알림 발송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}