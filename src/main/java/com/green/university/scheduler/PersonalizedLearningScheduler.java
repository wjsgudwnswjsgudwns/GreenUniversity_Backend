package com.green.university.scheduler;

import com.green.university.dto.PersonalizedLearningDTO;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.model.Student;
import com.green.university.service.PersonalizedLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 맞춤형 학습 지원 자동 분석 스케줄러
 * 매달 1일 새벽 2시에 전체 학생 대상 자동 분석 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalizedLearningScheduler {

    private final PersonalizedLearningService personalizedLearningService;
    private final StudentJpaRepository studentRepository;

    /**
     * 매달 1일 새벽 2시에 자동 분석 실행
     * Cron 표현식: 초 분 시 일 월 요일
     * "0 0 2 1 * *" = 매달 1일 02:00:00
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void runMonthlyAnalysis() {
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("========================================");
        log.info("=== 월간 자동 분석 시작: {} ===", startTime);
        log.info("========================================");

        try {
            // 전체 학생 목록 조회
            List<Student> allStudents = studentRepository.findAll();
            log.info("분석 대상 학생 수: {}명", allStudents.size());

            int successCount = 0;
            int failCount = 0;

            // 각 학생별로 분석 실행
            for (Student student : allStudents) {
                try {
                    log.info("분석 시작 - 학생 ID: {}, 이름: {}, 학과: {}",
                            student.getId(),
                            student.getName(),
                            student.getDepartment().getName());

                    // 기존 분석이 있더라도 새로 생성 (regenerate 로직 사용)
                    PersonalizedLearningDTO result =
                            personalizedLearningService.regenerateAnalysis(student.getId());

                    log.info("분석 완료 - 학생 ID: {}, GPA: {}, 분석일: {}",
                            student.getId(),
                            result.getLearningHistory().getOverallGPA(),
                            result.getAnalysisDate());

                    successCount++;

                    // 서버 부하 방지를 위한 대기 (학생 수가 많을 경우)
                    if (allStudents.size() > 100) {
                        Thread.sleep(1000); // 1초 대기
                    }

                } catch (Exception e) {
                    log.error("학생 분석 실패 - ID: {}, 이름: {}, 오류: {}",
                            student.getId(),
                            student.getName(),
                            e.getMessage());
                    failCount++;
                }
            }

            String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("========================================");
            log.info("=== 월간 자동 분석 완료: {} ===", endTime);
            log.info("=== 성공: {}명, 실패: {}명 ===", successCount, failCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("월간 자동 분석 중 치명적 오류 발생", e);
        }
    }

    /**
     * 테스트용 스케줄러 (매 5분마다 실행)
     * 운영 환경에서는 주석 처리 또는 제거 필요
     */
    // @Scheduled(cron = "0 */5 * * * *")
    public void runTestAnalysis() {
        log.info("========================================");
        log.info("=== 테스트 자동 분석 실행 ===");
        log.info("========================================");

        try {
            // 테스트용으로 첫 5명만 분석
            List<Student> students = studentRepository.findAll();
            List<Student> testStudents = students.stream().limit(5).toList();

            log.info("테스트 분석 대상: {}명", testStudents.size());

            for (Student student : testStudents) {
                try {
                    log.info("테스트 분석 - 학생 ID: {}, 이름: {}",
                            student.getId(),
                            student.getName());

                    personalizedLearningService.regenerateAnalysis(student.getId());

                    log.info("테스트 분석 완료 - 학생 ID: {}", student.getId());

                } catch (Exception e) {
                    log.error("테스트 분석 실패 - 학생 ID: {}, 오류: {}",
                            student.getId(),
                            e.getMessage());
                }
            }

            log.info("=== 테스트 자동 분석 완료 ===");

        } catch (Exception e) {
            log.error("테스트 분석 중 오류 발생", e);
        }
    }

    /**
     * 특정 학과 학생들만 분석 (필요시 사용)
     */
    public void runAnalysisForDepartment(Integer departmentId) {
        log.info("========================================");
        log.info("=== 학과별 자동 분석 시작: 학과 ID {} ===", departmentId);
        log.info("========================================");

        try {
            List<Student> students = studentRepository.findByDeptId(departmentId);
            log.info("분석 대상 학생 수: {}명", students.size());

            int successCount = 0;
            int failCount = 0;

            for (Student student : students) {
                try {
                    personalizedLearningService.regenerateAnalysis(student.getId());
                    successCount++;

                    if (students.size() > 50) {
                        Thread.sleep(1000);
                    }

                } catch (Exception e) {
                    log.error("학생 분석 실패 - ID: {}", student.getId(), e);
                    failCount++;
                }
            }

            log.info("=== 학과별 분석 완료 - 성공: {}명, 실패: {}명 ===", successCount, failCount);

        } catch (Exception e) {
            log.error("학과별 분석 중 오류 발생", e);
        }
    }

    /**
     * 특정 학년 학생들만 분석 (필요시 사용)
     */
    public void runAnalysisForGrade(Integer grade) {
        log.info("========================================");
        log.info("=== 학년별 자동 분석 시작: {}학년 ===", grade);
        log.info("========================================");

        try {
            List<Student> students = studentRepository.findByGrade(grade);
            log.info("분석 대상 학생 수: {}명", students.size());

            int successCount = 0;
            int failCount = 0;

            for (Student student : students) {
                try {
                    personalizedLearningService.regenerateAnalysis(student.getId());
                    successCount++;

                    if (students.size() > 50) {
                        Thread.sleep(1000);
                    }

                } catch (Exception e) {
                    log.error("학생 분석 실패 - ID: {}", student.getId(), e);
                    failCount++;
                }
            }

            log.info("=== 학년별 분석 완료 - 성공: {}명, 실패: {}명 ===", successCount, failCount);

        } catch (Exception e) {
            log.error("학년별 분석 중 오류 발생", e);
        }
    }
}