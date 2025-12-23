package com.green.university.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.response.MyGradeDto;
import com.green.university.repository.model.*;
import com.green.university.utils.Define;
import com.green.university.utils.DateUtil;

/**
 * 챗봇 서비스
 * 학생의 등록 여부, 수강 신청, 취득 학점, 졸업 요건 등을 조회하여 답변을 생성합니다.
 */
@Service
public class ChatbotService {

    @Autowired
    private UserService userService;

    @Autowired
    private TuitionService tuitionService;

    @Autowired
    private StuSubService stuSubService;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private StuStatService stuStatService;

    @Autowired
    private BreakAppService breakAppService;

    /**
     * 사용자 메시지를 분석하고 적절한 답변을 생성합니다.
     */
    @Transactional(readOnly = true)
    public String processMessage(Integer studentId, String message) {
        if (message == null || message.trim().isEmpty()) {
            return "안녕하세요! 무엇을 도와드릴까요?";
        }

        String lowerMessage = message.toLowerCase().trim();

        // 인사말 (가장 먼저 체크)
        if (containsAny(lowerMessage, "안녕", "하이", "hello", "hi", "반가워", "처음")) {
            return "안녕하세요! 그린대학교 챗봇입니다. 등록 여부, 수강 신청, 학점, 졸업 요건 등에 대해 물어보실 수 있습니다.";
        }

        // 휴학 신청 내역 관련 키워드 (수강 신청보다 먼저 체크 - "신청" 키워드 충돌 방지)
        if (containsAny(lowerMessage, "휴학", "휴학신청", "휴학내역", "휴학신청내역")) {
            return getBreakApplicationInfo(studentId);
        }

        // 학기별 성적 관련 키워드 (학점보다 먼저 체크 - "성적" 키워드 충돌 방지)
        if (containsAny(lowerMessage, "학기별", "학기별성적", "학기성적", "학기별성적조회")) {
            return getSemesterGradeInfo(studentId);
        }

        // 취득 학점 관련 키워드 (구체적으로 - 학기별보다 먼저 체크하지 않음, 학기별이 먼저 체크되므로)
        if (containsAny(lowerMessage, "취득학점", "취득 학점", "취득학점조회", "취득 학점 조회")) {
            return getGradeInfo(studentId);
        }

        // 등록 여부 관련 키워드
        if (containsAny(lowerMessage, "등록여부", "등록 여부", "등록상태", "등록 상태", "등록 확인", "등록여부 확인")) {
            return getRegistrationStatus(studentId);
        }

        // 수강 신청 관련 키워드 (구체적으로 - 휴학보다 먼저 체크하지 않음, 휴학이 먼저 체크되므로)
        if (containsAny(lowerMessage, "수강신청", "수강 신청", "수강내역", "수강 내역", "수강목록", "수강 목록", "수강 신청 내역")) {
            return getCourseEnrollmentInfo(studentId);
        }

        // 특정 강의 상세 정보 요청인지 먼저 확인 (예: "1번 강의", "2번 강의", "1번")
        // 시간표 조회보다 먼저 체크해야 함
        // "번" 키워드가 있고 숫자가 포함되어 있으면 강의 상세 정보 조회
        if (lowerMessage.contains("번") && lowerMessage.matches(".*\\d+.*")) {
            return getSubjectDetail(studentId, message);
        }

        // 시간표 관련 키워드
        if (containsAny(lowerMessage, "시간표", "나의시간표", "수강시간표", "강의시간표", "시간표 조회")) {
            return getScheduleList(studentId);
        }

        // 학적 상태 관련 키워드
        if (containsAny(lowerMessage, "학적", "학적상태", "학적정보", "학적 상태", "학적 상태 조회")) {
            return getStudentStatusInfo(studentId);
        }

        // 졸업 요건 관련 키워드
        if (containsAny(lowerMessage, "졸업", "졸업요건", "졸업조건", "졸업학점", "졸업가능", "졸업 요건 확인")) {
            return getGraduationRequirements(studentId);
        }

        // 일반 학점 관련 키워드 (마지막에 체크 - 취득학점, 학기별성적보다 덜 구체적)
        if (containsAny(lowerMessage, "학점", "평점", "학점평균", "평균")) {
            return getGradeInfo(studentId);
        }

        // 도움말
        if (containsAny(lowerMessage, "도움", "도와", "help", "무엇", "뭐", "어떻게", "기능")) {
            return getHelpMessage();
        }

        // 기본 응답
        return "죄송합니다. 질문을 이해하지 못했습니다. 다음 중 하나를 물어보세요:\n" +
               "• 등록 여부\n" +
               "• 수강 신청 내역\n" +
               "• 취득 학점\n" +
               "• 졸업 요건\n" +
               "• 휴학 내역\n" +
               "• 시간표 조회\n" +
               "• 학적 상태\n" +
               "• 학기별 성적";
    }

    /**
     * 등록 여부 조회 (등록 완료 여부, 제출 여부, 제출 기한)
     */
    private String getRegistrationStatus(Integer studentId) {
        try {
            Tuition tuition = tuitionService.readByStudentIdAndSemester(
                    studentId, Define.getCurrentYear(), Define.getCurrentSemester());

            if (tuition == null) {
                return String.format("【%d년 %d학기 등록 현황】\n\n" +
                        "등록금 고지서가 아직 발급되지 않았습니다.\n" +
                        "등록금 고지서 발급 후 등록금을 납부해주세요.", 
                        Define.getCurrentYear(), Define.getCurrentSemester());
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%d년 %d학기 등록 현황】\n\n", 
                    Define.getCurrentYear(), Define.getCurrentSemester()));

            if (tuition.getStatus() != null && tuition.getStatus()) {
                response.append("등록 상태: ✅ 등록 완료\n");
                response.append("등록금 제출: ✅ 제출 완료\n");
            } else {
                response.append("등록 상태: ❌ 미등록\n");
                response.append("등록금 제출: ❌ 미제출\n");
                response.append(String.format("등록금액: %s원\n", formatNumber(tuition.getTuiAmount())));
                if (tuition.getSchAmount() != null && tuition.getSchAmount() > 0) {
                    response.append(String.format("장학금액: %s원\n", formatNumber(tuition.getSchAmount())));
                    int paymentAmount = tuition.getTuiAmount() - tuition.getSchAmount();
                    response.append(String.format("납부금액: %s원\n", formatNumber(paymentAmount)));
                } else {
                    response.append(String.format("납부금액: %s원\n", formatNumber(tuition.getTuiAmount())));
                }
                response.append("\n※ 등록금 납부 기한: 학기 시작 전까지\n");
                response.append("등록금 납부 페이지에서 납부하실 수 있습니다.");
            }

            return response.toString();
        } catch (Exception e) {
            return "등록 정보를 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 수강 신청 내역 조회
     */
    private String getCourseEnrollmentInfo(Integer studentId) {
        try {
            List<StuSub> stuSubList = stuSubService.readStuSubList(studentId);

            if (stuSubList.isEmpty()) {
                return String.format("%d년 %d학기 수강 신청 내역이 없습니다.", 
                        Define.getCurrentYear(), Define.getCurrentSemester());
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%d년 %d학기 수강 신청 내역】\n\n", 
                    Define.getCurrentYear(), Define.getCurrentSemester()));

            int totalCredits = 0;
            for (StuSub stuSub : stuSubList) {
                Subject subject = stuSub.getSubject();
                if (subject != null) {
                    response.append(String.format("• %s (%s학점)\n", 
                            subject.getName(), subject.getGrades()));
                    totalCredits += subject.getGrades();
                }
            }

            response.append(String.format("\n총 신청 학점: %d학점", totalCredits));

            return response.toString();
        } catch (Exception e) {
            return "수강 신청 내역을 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 학점 정보 조회 (이번 학기만)
     */
    private String getGradeInfo(Integer studentId) {
        try {
            // 현재 학기 성적
            MyGradeDto currentSemester = gradeService.readMyGradeByStudentId(studentId);

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%d년 %d학기 학점】\n\n", 
                    Define.getCurrentYear(), Define.getCurrentSemester()));

            if (currentSemester != null && currentSemester.getSumGrades() > 0) {
                response.append(String.format("신청 학점: %d학점\n", currentSemester.getSumGrades()));
                response.append(String.format("취득 학점: %d학점\n", currentSemester.getMyGrades()));
                if (currentSemester.getAverage() > 0) {
                    response.append(String.format("평점 평균: %.2f", currentSemester.getAverage()));
                }
            } else {
                response.append("이번 학기 수강한 과목이 없습니다.");
            }

            return response.toString();
        } catch (Exception e) {
            return "학점 정보를 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 졸업 요건 조회
     */
    private String getGraduationRequirements(Integer studentId) {
        try {
            List<MyGradeDto> totalGrades = gradeService.readgradeinquiryList(studentId);
            
            int totalMyGrades = 0;
            if (totalGrades != null && !totalGrades.isEmpty()) {
                totalMyGrades = totalGrades.stream()
                        .mapToInt(MyGradeDto::getMyGrades)
                        .sum();
            }

            // 기본 졸업 요건 (일반적으로 130학점 이상)
            int requiredCredits = 130;
            int remainingCredits = Math.max(0, requiredCredits - totalMyGrades);

            StringBuilder response = new StringBuilder();
            response.append("【졸업 요건】\n\n");
            response.append(String.format("졸업 필요 학점: %d학점\n", requiredCredits));
            response.append(String.format("현재 취득 학점: %d학점\n", totalMyGrades));
            response.append(String.format("부족한 학점: %d학점\n\n", remainingCredits));

            if (remainingCredits == 0) {
                response.append("✅ 졸업 요건을 충족하셨습니다!");
            } else {
                response.append(String.format("⚠️ 졸업까지 %d학점이 더 필요합니다.", remainingCredits));
            }

            // 추가 정보
            response.append("\n\n※ 참고사항:");
            response.append("\n- 평점 평균 2.0 이상 필요");
            response.append("\n- 전공 필수 과목 이수 확인 필요");
            response.append("\n- 자세한 졸업 요건은 학과 사무실에 문의하세요.");

            return response.toString();
        } catch (Exception e) {
            return "졸업 요건을 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 휴학 신청 내역 조회 (휴학 상태인지 아닌지만 한 줄로)
     */
    private String getBreakApplicationInfo(Integer studentId) {
        try {
            StuStat stuStat = stuStatService.readCurrentStatus(studentId);
            String status = stuStat.getStatus();
            
            if ("휴학".equals(status)) {
                return "현재 상태: ✅ 휴학 중";
            } else if ("재학".equals(status)) {
                return "현재 상태: ✅ 재학 중";
            } else {
                return String.format("현재 상태: %s", status);
            }
        } catch (Exception e) {
            return "휴학 상태를 조회하는 중 오류가 발생했습니다.";
        }
    }


    /**
     * 시간표 조회 (강의 리스트만)
     */
    private String getScheduleList(Integer studentId) {
        try {
            List<StuSub> stuSubList = stuSubService.readStuSubList(studentId);

            if (stuSubList.isEmpty()) {
                return String.format("%d년 %d학기 수강 신청 내역이 없습니다.", 
                        Define.getCurrentYear(), Define.getCurrentSemester());
            }

            StringBuilder response = new StringBuilder();
            response.append(String.format("【%d년 %d학기 수강 과목】\n\n", 
                    Define.getCurrentYear(), Define.getCurrentSemester()));
            response.append("아래 버튼을 클릭하여 각 강의의 상세 정보를 확인하세요.\n\n");

            int index = 1;
            for (StuSub stuSub : stuSubList) {
                Subject subject = stuSub.getSubject();
                if (subject != null) {
                    response.append(String.format("%d. %s\n", index, subject.getName()));
                    index++;
                }
            }

            return response.toString();
        } catch (Exception e) {
            return "시간표를 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 특정 강의 상세 정보 조회
     */
    private String getSubjectDetail(Integer studentId, String message) {
        try {
            List<StuSub> stuSubList = stuSubService.readStuSubList(studentId);

            if (stuSubList.isEmpty()) {
                return "수강 신청 내역이 없습니다.";
            }

            // 메시지에서 번호 추출
            int subjectIndex = -1;
            String[] parts = message.replaceAll("[^0-9]", " ").trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty()) {
                try {
                    subjectIndex = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                    // 번호를 찾을 수 없음
                }
            }

            if (subjectIndex < 1 || subjectIndex > stuSubList.size()) {
                return "강의 번호를 찾을 수 없습니다. 시간표를 다시 조회해주세요.";
            }

            StuSub stuSub = stuSubList.get(subjectIndex - 1);
            Subject subject = stuSub.getSubject();

            if (subject == null) {
                return "강의 정보를 찾을 수 없습니다.";
            }

            StringBuilder response = new StringBuilder();
            response.append("【강의 상세 정보】\n\n");
            response.append(String.format("강의명: %s\n", subject.getName()));
            response.append(String.format("학수번호: %d\n", subject.getId()));
            response.append(String.format("학점: %d학점\n", subject.getGrades()));
            
            if (subject.getSubDay() != null) {
                response.append(String.format("요일: %s\n", subject.getSubDay()));
            }
            if (subject.getStartTime() != null && subject.getEndTime() != null) {
                response.append(String.format("시간: %02d:00-%02d:00\n", 
                        subject.getStartTime(), subject.getEndTime()));
            }
            if (subject.getRoom() != null) {
                response.append(String.format("강의실: %s\n", subject.getRoom().getId()));
            }
            if (subject.getProfessor() != null) {
                response.append(String.format("담당교수: %s\n", subject.getProfessor().getName()));
            }
            if (subject.getType() != null) {
                response.append(String.format("강의구분: %s\n", subject.getType()));
            }

            return response.toString();
        } catch (Exception e) {
            return "강의 상세 정보를 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 학적 상태 조회 (재학/복학/휴학/학사경고 구별)
     */
    private String getStudentStatusInfo(Integer studentId) {
        try {
            Student student = userService.readStudent(studentId);
            StuStat stuStat = stuStatService.readCurrentStatus(studentId);
            String status = stuStat.getStatus();

            StringBuilder response = new StringBuilder();
            response.append("【학적 상태】\n\n");
            response.append(String.format("학번: %d\n", studentId));
            response.append(String.format("이름: %s\n\n", student.getName()));
            
            // 상태별 구분
            if ("재학".equals(status)) {
                response.append("현재 상태: ✅ 재학 중\n");
            } else if ("복학".equals(status)) {
                response.append("현재 상태: ✅ 복학 중\n");
            } else if ("휴학".equals(status)) {
                response.append("현재 상태: ⏸️ 휴학 중\n");
            } else if ("학사경고".equals(status) || status != null && status.contains("경고")) {
                response.append("현재 상태: ⚠️ 학사경고\n");
            } else if ("졸업".equals(status)) {
                response.append("현재 상태: 🎓 졸업\n");
            } else if ("자퇴".equals(status)) {
                response.append("현재 상태: ❌ 자퇴\n");
            } else {
                response.append(String.format("현재 상태: %s\n", status));
            }

            return response.toString();
        } catch (Exception e) {
            return "학적 상태를 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 학기별 성적 조회 (이번 학기 제외한 나머지 학기들 + 전체 누계)
     */
    private String getSemesterGradeInfo(Integer studentId) {
        try {
            List<MyGradeDto> semesterGrades = gradeService.readgradeinquiryList(studentId);

            if (semesterGrades == null || semesterGrades.isEmpty()) {
                return "수강한 기록이 없습니다.";
            }

            StringBuilder response = new StringBuilder();
            response.append("【학기별 성적】\n\n");

            // 현재 학기 제외한 나머지 학기들
            List<MyGradeDto> pastSemesters = semesterGrades.stream()
                    .filter(g -> !(g.getSubYear().equals(Define.getCurrentYear()) && 
                                  g.getSemester().equals(Define.getCurrentSemester())))
                    .collect(java.util.stream.Collectors.toList());

            if (pastSemesters.isEmpty()) {
                response.append("이번 학기 외 수강한 기록이 없습니다.\n\n");
            } else {
                for (MyGradeDto grade : pastSemesters) {
                    response.append(String.format("【%d년 %d학기】\n", 
                            grade.getSubYear(), grade.getSemester()));
                    response.append(String.format("신청 학점: %d학점\n", grade.getSumGrades()));
                    response.append(String.format("취득 학점: %d학점\n", grade.getMyGrades()));
                    if (grade.getAverage() > 0) {
                        response.append(String.format("평점 평균: %.2f\n", grade.getAverage()));
                    }
                    response.append("\n");
                }
            }

            // 전체 누계
            int totalSumGrades = semesterGrades.stream()
                    .mapToInt(MyGradeDto::getSumGrades)
                    .sum();
            int totalMyGrades = semesterGrades.stream()
                    .mapToInt(MyGradeDto::getMyGrades)
                    .sum();

            double totalAvg = semesterGrades.stream()
                    .filter(g -> g.getAverage() > 0)
                    .mapToDouble(MyGradeDto::getAverage)
                    .average()
                    .orElse(0.0);

            response.append("【전체 누계】\n");
            response.append(String.format("총 신청 학점: %d학점\n", totalSumGrades));
            response.append(String.format("총 취득 학점: %d학점\n", totalMyGrades));
            if (totalAvg > 0) {
                response.append(String.format("전체 평점 평균: %.2f", totalAvg));
            }

            return response.toString();
        } catch (Exception e) {
            return "학기별 성적을 조회하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 도움말 메시지
     */
    private String getHelpMessage() {
        return "【챗봇 사용 안내】\n\n" +
               "다음과 같은 질문을 하실 수 있습니다:\n\n" +
               "📋 등록 관련:\n" +
               "  • 등록 여부 확인\n\n" +
               "📚 수강 신청 관련:\n" +
               "  • 수강 신청 내역\n" +
               "  • 시간표 조회\n\n" +
               "📊 학점 관련:\n" +
               "  • 취득 학점 조회 (이번 학기)\n" +
               "  • 학기별 성적 조회 (과거 학기 + 전체 누계)\n\n" +
               "🎓 졸업 관련:\n" +
               "  • 졸업 요건 확인\n\n" +
               "📝 기타:\n" +
               "  • 휴학 내역 조회\n" +
               "  • 학적 상태 조회\n\n" +
               "원하시는 내용을 자유롭게 질문해주세요!";
    }

    /**
     * 문자열에 키워드가 포함되어 있는지 확인
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 숫자를 천 단위 구분자로 포맷팅
     */
    private String formatNumber(Integer number) {
        if (number == null) return "0";
        return String.format("%,d", number);
    }
}

