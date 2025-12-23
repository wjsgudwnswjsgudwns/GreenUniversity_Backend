package com.green.university.service;

import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Subject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEmailService {

    private final JavaMailSender mailSender;

    /**
     * 학생에게 위험 알림 이메일 발송
     */
    public void sendRiskEmailToStudent(Student student, Subject subject, String riskLevel, AIAnalysisResult result) {
        try {
            String studentEmail = student.getEmail();
            if (studentEmail == null || studentEmail.trim().isEmpty()) {
                log.warn("학생 이메일이 없습니다. 학생 ID: {}", student.getId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(studentEmail);
            helper.setSubject("[Green University] 학업 상담 안내");

            String htmlContent = createStudentEmailTemplate(
                    student.getName(),
                    subject.getName(),
                    riskLevel,
                    result
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("학생 위험 알림 이메일 발송 완료: 학생={}, 이메일={}, 위험도={}",
                    student.getName(), studentEmail, riskLevel);

        } catch (MessagingException e) {
            log.error("학생 이메일 발송 실패: 학생 ID={}, 오류={}", student.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("학생 이메일 발송 중 예외 발생: 학생 ID={}, 오류={}", student.getId(), e.getMessage(), e);
        }
    }

    /**
     * 지도교수에게 위험 알림 이메일 발송
     */
    public void sendRiskEmailToProfessor(Student student, Subject subject, String riskLevel, AIAnalysisResult result) {
        try {
            if (student.getAdvisor() == null) {
                log.warn("학생의 지도교수 정보가 없습니다. 학생 ID: {}", student.getId());
                return;
            }

            String professorEmail = student.getAdvisor().getEmail();
            if (professorEmail == null || professorEmail.trim().isEmpty()) {
                log.warn("지도교수 이메일이 없습니다. 교수 ID: {}", student.getAdvisor().getId());
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(professorEmail);
            helper.setSubject("[Green University] 학생 상담 요청");

            String htmlContent = createProfessorEmailTemplate(
                    student.getAdvisor().getName(),
                    student.getName(),
                    student.getId(),
                    subject.getName(),
                    riskLevel,
                    result
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("교수 위험 알림 이메일 발송 완료: 교수={}, 이메일={}, 학생={}, 위험도={}",
                    student.getAdvisor().getName(), professorEmail, student.getName(), riskLevel);

        } catch (MessagingException e) {
            log.error("교수 이메일 발송 실패: 학생 ID={}, 오류={}", student.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("교수 이메일 발송 중 예외 발생: 학생 ID={}, 오류={}", student.getId(), e.getMessage(), e);
        }
    }

    /**
     * 학생용 이메일 HTML 템플릿
     */
    private String createStudentEmailTemplate(String studentName, String subjectName,
                                              String riskLevel, AIAnalysisResult result) {
        String riskLevelKorean = getRiskLevelKorean(riskLevel);
        String riskColor = getRiskColor(riskLevel);
        String riskIcon = getRiskIcon(riskLevel);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Malgun Gothic', '맑은 고딕', Arial, sans-serif; background-color: #f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #216d30 0%%, #1a5524 100%%); padding: 30px; text-align: center;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: bold;">
                                            🎓 Green University
                                        </h1>
                                        <p style="margin: 10px 0 0 0; color: #e8f5e9; font-size: 14px;">학업 상담 안내</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <p style="margin: 0 0 20px 0; font-size: 16px; color: #333333;">
                                            <strong>%s</strong> 학생님, 안녕하세요.
                                        </p>
                                        
                                        <!-- Risk Alert Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: %s; border-radius: 8px; margin: 20px 0; border-left: 4px solid %s;">
                                            <tr>
                                                <td style="padding: 20px;">
                                                    <div style="font-size: 24px; margin-bottom: 10px;">%s</div>
                                                    <p style="margin: 0; font-size: 18px; font-weight: bold; color: #333333;">
                                                        학업 상태: <span style="color: %s;">%s</span>
                                                    </p>
                                                    <p style="margin: 10px 0 0 0; font-size: 14px; color: #666666;">
                                                        과목: <strong>%s</strong>
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                            <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #216d30;">📊 상세 현황</h3>
                                            <table width="100%%" cellpadding="8" cellspacing="0" style="font-size: 14px;">
                                                <tr>
                                                    <td style="width: 40%%; color: #666666;">출결 상태</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">과제 상태</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">중간고사</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">기말고사</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">등록금 납부</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                            </table>
                                        </div>
                                        
                                        <!-- AI Analysis -->
                                        %s
                                        
                                        <!-- Action Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #fff3cd; border-radius: 8px; margin: 20px 0; border: 1px solid #ffc107;">
                                            <tr>
                                                <td style="padding: 20px;">
                                                    <p style="margin: 0 0 10px 0; font-size: 16px; font-weight: bold; color: #856404;">
                                                        💡 권장 사항
                                                    </p>
                                                    <p style="margin: 0; font-size: 14px; color: #856404; line-height: 1.6;">
                                                        지도교수님과의 상담을 통해 학업 개선 방안을 논의하시기 바랍니다.<br>
                                                        조기에 대응할수록 더 좋은 결과를 얻을 수 있습니다.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="margin: 30px 0 0 0; font-size: 14px; color: #666666; line-height: 1.6;">
                                            학업에 어려움이 있으시면 언제든지 지도교수님이나 학생상담센터로 연락주시기 바랍니다.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e0e6ed;">
                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            Green University 학사관리시스템<br>
                                            본 메일은 자동 발송되었습니다.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
                studentName,
                getRiskBackgroundColor(riskLevel), riskColor,
                riskIcon, riskColor, riskLevelKorean,
                subjectName,
                getStatusColor(result.getAttendanceStatus()), getStatusKorean(result.getAttendanceStatus()),
                getStatusColor(result.getHomeworkStatus()), getStatusKorean(result.getHomeworkStatus()),
                getStatusColor(result.getMidtermStatus()), getStatusKorean(result.getMidtermStatus()),
                getStatusColor(result.getFinalStatus()), getStatusKorean(result.getFinalStatus()),
                getStatusColor(result.getTuitionStatus()), getStatusKorean(result.getTuitionStatus()),
                getAIAnalysisSection(result.getAnalysisDetail())
        );
    }

    /**
     * 교수용 이메일 HTML 템플릿
     */
    private String createProfessorEmailTemplate(String professorName, String studentName,
                                                Integer studentId, String subjectName,
                                                String riskLevel, AIAnalysisResult result) {
        String riskLevelKorean = getRiskLevelKorean(riskLevel);
        String riskColor = getRiskColor(riskLevel);
        String riskIcon = getRiskIcon(riskLevel);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Malgun Gothic', '맑은 고딕', Arial, sans-serif; background-color: #f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f4; padding: 20px;">
                    <tr>
                        <td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); overflow: hidden;">
                                <!-- Header -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #216d30 0%%, #1a5524 100%%); padding: 30px; text-align: center;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: bold;">
                                            🎓 Green University
                                        </h1>
                                        <p style="margin: 10px 0 0 0; color: #e8f5e9; font-size: 14px;">학생 상담 요청</p>
                                    </td>
                                </tr>
                                
                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <p style="margin: 0 0 20px 0; font-size: 16px; color: #333333;">
                                            <strong>%s</strong> 교수님, 안녕하세요.
                                        </p>
                                        
                                        <p style="margin: 0 0 20px 0; font-size: 14px; color: #666666; line-height: 1.6;">
                                            지도학생의 학업 상태에 대한 알림을 드립니다.
                                        </p>
                                        
                                        <!-- Student Info Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f8f9fa; border-radius: 8px; margin: 20px 0; border: 1px solid #e0e6ed;">
                                            <tr>
                                                <td style="padding: 20px;">
                                                    <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #216d30;">👤 학생 정보</h3>
                                                    <table width="100%%" cellpadding="8" cellspacing="0" style="font-size: 14px;">
                                                        <tr>
                                                            <td style="width: 30%%; color: #666666;">학생 이름</td>
                                                            <td style="font-weight: bold;">%s</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="color: #666666;">학번</td>
                                                            <td style="font-weight: bold;">%d</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="color: #666666;">과목</td>
                                                            <td style="font-weight: bold;">%s</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <!-- Risk Alert Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: %s; border-radius: 8px; margin: 20px 0; border-left: 4px solid %s;">
                                            <tr>
                                                <td style="padding: 20px;">
                                                    <div style="font-size: 24px; margin-bottom: 10px;">%s</div>
                                                    <p style="margin: 0; font-size: 18px; font-weight: bold; color: #333333;">
                                                        위험도: <span style="color: %s;">%s</span>
                                                    </p>
                                                    <p style="margin: 10px 0 0 0; font-size: 14px; color: #666666;">
                                                        상담이 필요한 상태입니다.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                            <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #216d30;">📊 학생 상세 현황</h3>
                                            <table width="100%%" cellpadding="8" cellspacing="0" style="font-size: 14px;">
                                                <tr>
                                                    <td style="width: 40%%; color: #666666;">출결 상태</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">과제 상태</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">중간고사</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">기말고사</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">등록금 납부</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                                <tr>
                                                    <td style="color: #666666;">상담 이력</td>
                                                    <td style="font-weight: bold; color: %s;">%s</td>
                                                </tr>
                                            </table>
                                        </div>
                                        
                                        <!-- AI Analysis -->
                                        %s
                                        
                                        <!-- Action Box -->
                                        <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #fff3cd; border-radius: 8px; margin: 20px 0; border: 1px solid #ffc107;">
                                            <tr>
                                                <td style="padding: 20px;">
                                                    <p style="margin: 0 0 10px 0; font-size: 16px; font-weight: bold; color: #856404;">
                                                        💡 권장 조치 사항
                                                    </p>
                                                    <p style="margin: 0; font-size: 14px; color: #856404; line-height: 1.6;">
                                                        • 해당 학생과 개별 상담 일정을 잡아주시기 바랍니다.<br>
                                                        • 학업 부진의 원인을 파악하고 개선 방안을 함께 모색해주세요.<br>
                                                        • 필요시 학생상담센터에 전문 상담을 의뢰할 수 있습니다.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                        
                                        <p style="margin: 30px 0 0 0; font-size: 14px; color: #666666; line-height: 1.6;">
                                            학생 지도에 협조해 주셔서 감사합니다.<br>
                                            문의사항은 학사관리팀으로 연락주시기 바랍니다.
                                        </p>
                                    </td>
                                </tr>
                                
                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e0e6ed;">
                                        <p style="margin: 0; font-size: 12px; color: #999999;">
                                            Green University 학사관리시스템<br>
                                            본 메일은 자동 발송되었습니다.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
                professorName,
                studentName, studentId, subjectName,
                getRiskBackgroundColor(riskLevel), riskColor,
                riskIcon, riskColor, riskLevelKorean,
                getStatusColor(result.getAttendanceStatus()), getStatusKorean(result.getAttendanceStatus()),
                getStatusColor(result.getHomeworkStatus()), getStatusKorean(result.getHomeworkStatus()),
                getStatusColor(result.getMidtermStatus()), getStatusKorean(result.getMidtermStatus()),
                getStatusColor(result.getFinalStatus()), getStatusKorean(result.getFinalStatus()),
                getStatusColor(result.getTuitionStatus()), getStatusKorean(result.getTuitionStatus()),
                getStatusColor(result.getCounselingStatus()), getStatusKorean(result.getCounselingStatus()),
                getAIAnalysisSection(result.getAnalysisDetail())
        );
    }

    // Helper methods
    private String getRiskLevelKorean(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> "심각";
            case "RISK" -> "위험";
            case "CAUTION" -> "주의";
            default -> "정상";
        };
    }

    private String getRiskColor(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> "#dc3545";
            case "RISK" -> "#fd7e14";
            case "CAUTION" -> "#ffc107";
            default -> "#28a745";
        };
    }

    private String getRiskBackgroundColor(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> "#f8d7da";
            case "RISK" -> "#ffe5d0";
            case "CAUTION" -> "#fff3cd";
            default -> "#d4edda";
        };
    }

    private String getRiskIcon(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> "🚨";
            case "RISK" -> "⚠️";
            case "CAUTION" -> "⚡";
            default -> "✅";
        };
    }

    private String getStatusKorean(String status) {
        if (status == null) return "확인 필요";
        return switch (status) {
            case "CRITICAL" -> "심각";
            case "RISK" -> "위험";
            case "CAUTION" -> "주의";
            case "NORMAL" -> "양호";
            default -> status;
        };
    }

    private String getStatusColor(String status) {
        if (status == null) return "#999999";
        return switch (status) {
            case "CRITICAL" -> "#dc3545";
            case "RISK" -> "#fd7e14";
            case "CAUTION" -> "#ffc107";
            case "NORMAL" -> "#28a745";
            default -> "#999999";
        };
    }

    private String getAIAnalysisSection(String analysisDetail) {
        if (analysisDetail == null || analysisDetail.trim().isEmpty()) {
            return "";
        }

        return String.format("""
            <div style="background-color: #e8f5e9; border-radius: 8px; padding: 20px; margin: 20px 0; border-left: 4px solid #216d30;">
                <h3 style="margin: 0 0 15px 0; font-size: 16px; color: #216d30;">AI 분석 결과</h3>
                <p style="margin: 0; font-size: 14px; color: #333333; line-height: 1.6; white-space: pre-wrap;">%s</p>
            </div>
            """, analysisDetail);
    }
}