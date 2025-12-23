package com.green.university.service;

import java.util.List;

import com.green.university.repository.*;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.response.GradeForScholarshipDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.utils.Define;

@Service
public class TuitionService {

    @Autowired
    private TuitionJpaRepository tuitionJpaRepository;

    @Autowired
    private StuSchJpaRepository stuSchJpaRepository;

    @Autowired
    private CollTuitJpaRepository collTuitJpaRepository;

    @Autowired
    private StudentJpaRepository studentJpaRepository;

    @Autowired
    private StuSubDetailJpaRepository stuSubDetailJpaRepository;

    @Autowired
    private AIAnalysisResultService aiAnalysisResultService;

    @Autowired
    private StuStatService stuStatService;

    @Autowired
    private BreakAppService breakAppService;

    @Autowired
    private UserService userService;

    @Autowired
    private GradeService gradeService;

    @Autowired
    private ScholarshipJpaRepository scholarshipJpaRepository;

    /**
     * @param studentId (principal의 id와 동일)
     * @return 해당 학생의 모든 등록금 납부 내역 (최신 학기 → 과거 학기)
     */
    @Transactional(readOnly = true)
    public List<Tuition> readTuitionList(Integer studentId) {
        return tuitionJpaRepository
                .findByIdStudentIdOrderByIdTuiYearDescIdSemesterDesc(studentId);
    }

    /**
     * @param studentId (principal의 id와 동일)
     * @return 해당 학생의 납부 여부에 따른 등록금 납부 내역
     */
    @Transactional(readOnly = true)
    public List<Tuition> readTuitionListByStatus(Integer studentId, Boolean status) {
        return tuitionJpaRepository
                .findByIdStudentIdAndStatusOrderByIdTuiYearDescIdSemesterDesc(studentId, status);
    }

    /**
     * @return 해당 학생의 현재 학기 등록금 고지서
     */
    @Transactional(readOnly = true)
    public Tuition readByStudentIdAndSemester(Integer studentId, Integer tuiYear, Integer semester) {
        return tuitionJpaRepository
                .findByIdStudentIdAndIdTuiYearAndIdSemester(studentId, tuiYear, semester)
                .orElse(null);
    }

    // TuitionService에 추가
    @Transactional
    public void updateStudentSemester(Integer studentId) {
        Student student = studentJpaRepository.findById(studentId)
                .orElseThrow(() -> new CustomRestfullException(
                        "학생 정보를 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        // 현재 학기가 2학기이고, 학생이 1학기라면 → 2학기로 업데이트
        if (Define.getCurrentSemester() == 2 && student.getSemester() == 1) {
            student.setSemester(2);
            studentJpaRepository.save(student);
        }
        // 현재 학기가 1학기이고, 이전 학기가 2학기였다면 → 학년 올리고 1학기로
        else if (Define.getCurrentSemester() == 1 && student.getSemester() == 2) {
            student.setGrade(student.getGrade() + 1);
            student.setSemester(1);
            studentJpaRepository.save(student);
        }
    }

    /**
     * 장학금 유형 결정 (stu_sch_tb insert)
     */
    @Transactional
    public Integer createCurrentSchType(Integer studentId) {

        StuSchId stuSchId = new StuSchId();
        stuSchId.setStudentId(studentId);
        stuSchId.setSchYear(Define.getCurrentYear());
        stuSchId.setSemester(Define.getCurrentSemester());

        StuSch stuSch = new StuSch();
        stuSch.setId(stuSchId);

        Student studentEntity = userService.readStudent(studentId);

        int currentSystemYear = Define.getCurrentYear();
        int currentSystemSemester = Define.getCurrentSemester();

        // 1학년 1학기 학생은 무조건 2유형
        if (studentEntity.getGrade() == 1 && studentEntity.getSemester() == 1) {
            stuSch.setSchType(2);
            Scholarship scholarship = scholarshipJpaRepository.findById(2)
                    .orElse(null);
            stuSch.setScholarship(scholarship);

            stuSchJpaRepository.save(stuSch);
            return 2;
        }

        // 직전 학기 계산
        int prevYear = currentSystemYear;
        int prevSemester = currentSystemSemester - 1;

        if (prevSemester == 0) {
            prevYear--;
            prevSemester = 2;
        }

        // 직전 학기 성적 조회
        GradeForScholarshipDto gradeDto = gradeService.readAvgGrade(studentId, prevYear, prevSemester);

        // 직전 학기 성적이 없다면: 장학 없음
        if (gradeDto == null) {
            stuSchJpaRepository.save(stuSch);
            return null;
        }

        Double avgGrade = gradeDto.getAvgGrade();

        System.out.println("학생 " + studentId + " 직전학기(" + prevYear + "-" + prevSemester + ") 평균: " + avgGrade);

        // 평점에 따라 장학금 유형 결정
        Integer schType = null;
        if (avgGrade >= 4.2) {
            schType = 1;
            stuSch.setSchType(1);
        } else if (avgGrade >= 3.7) {
            schType = 2;
            stuSch.setSchType(2);
        }

        if (schType != null) {
            Scholarship scholarship = scholarshipJpaRepository.findById(schType)
                    .orElse(null);
            stuSch.setScholarship(scholarship);
        }


        stuSchJpaRepository.save(stuSch);
        return stuSch.getSchType();
    }

    /**
     * 등록금 고지서 생성 (교직원 탭에서 사용)
     *
     * @param studentId (principal의 id와 동일)
     */
    @Transactional
    public int createTuition(Integer studentId) {

        updateStudentSemester(studentId); // 진급

        // 1. 학적 상태가 '졸업' 또는 '자퇴'라면 생성하지 않음
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentId);
        if ("졸업".equals(stuStatEntity.getStatus())
                || "자퇴".equals(stuStatEntity.getStatus())) {
            return 0;
        }

        // 2. 현재 학기 휴학 승인 상태라면 생성하지 않음
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentId);
        for (BreakApp b : breakAppList) {
            if ("승인".equals(b.getStatus())) {
                if (b.getToYear() > Define.getCurrentYear()) {  // ✅ 수정
                    return 0;
                } else if (b.getToYear() == Define.getCurrentYear()
                        && b.getToSemester() >= Define.getCurrentSemester()) {  // ✅ 수정
                    return 0;
                }
            }
        }

// 3. 이미 해당 학기의 등록금 고지서가 존재한다면 생성하지 않음
        if (readByStudentIdAndSemester(
                studentId, Define.getCurrentYear(), Define.getCurrentSemester()) != null) {  // ✅ 수정
            return 0;
        }

        // 4. 등록금액 조회 (student → department → college → coll_tuit_tb)
        Integer tuiAmount = resolveTuitionAmountByStudent(studentId);
        if (tuiAmount == null) {
            throw new CustomRestfullException("등록금 금액을 찾을 수 없습니다.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 5. 장학금 유형 결정 + 유형 반환 (null이면 장학금 지원 대상이 아님)
        Integer schType = createCurrentSchType(studentId);

        // 6. 장학금액 계산
        Integer schAmount = 0;
        if (schType != null) {
            var stuSchOpt = stuSchJpaRepository.findWithScholarship(
                    studentId, Define.getCurrentYear(), Define.getCurrentSemester());

            if (stuSchOpt.isPresent() && stuSchOpt.get().getScholarship() != null) {
                Integer maxAmount = stuSchOpt.get().getScholarship().getMaxAmount();
                if (maxAmount != null) {
                    // 등록금액보다 최대 장학금액이 크면 등록금액만큼만 장학
                    schAmount = Math.min(tuiAmount, maxAmount);
                }
            }
        }

        // 7. EmbeddedId 생성 후 Tuition 엔티티 저장
        TuitionId tuitionId = new TuitionId();
        tuitionId.setStudentId(studentId);
        tuitionId.setTuiYear(Define.getCurrentYear());      // ✅ 수정
        tuitionId.setSemester(Define.getCurrentSemester()); // ✅ 수정

        Tuition tuition = new Tuition();
        tuition.setId(tuitionId);
        tuition.setTuiAmount(tuiAmount);
        tuition.setSchType(schType);
        tuition.setSchAmount(schAmount);
        tuition.setStatus(false); // 최초 생성 시 미납

        tuitionJpaRepository.save(tuition);

        // JPA라 rowCount는 없으니, 생성 성공 기준으로 1 반환
        return 1;
    }

    /**
     * 등록금 납부
     */
    @Transactional
    public void updateStatus(Integer studentId) {
        System.out.println("=== 등록금 납부 처리 시작 ===");

        TuitionId id = new TuitionId();
        id.setStudentId(studentId);
        id.setTuiYear(Define.getCurrentYear());
        id.setSemester(Define.getCurrentSemester());

        Tuition tuition = tuitionJpaRepository.findById(id)
                .orElseThrow(() -> new CustomRestfullException(
                        "해당 학기 등록금 고지서를 찾을 수 없습니다.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        tuition.setStatus(true);

        String status = stuStatService.readCurrentStatus(studentId).getStatus();
        if ("휴학".equals(status)) {
            stuStatService.updateStatus(studentId, "재학", "9999-01-01", null);
        }

        // 등록금 납부 후 AI 분석 트리거 추가
//        triggerAIAnalysisForTuition(studentId);

        System.out.println("=== 등록금 납부 처리 완료 ===");
    }

    /**
     * ✅ 등록금 납부 후 해당 학생의 모든 과목 AI 분석
     */
//    private void triggerAIAnalysisForTuition(Integer studentId) {
//        try {
//            System.out.println("🤖 등록금 납부 후 AI 분석 시작: 학생 " + studentId);
//
//            List<StuSubDetail> enrollments = stuSubDetailJpaRepository
//                    .findByStudentIdWithRelations(studentId);
//
//            int successCount = 0;
//            for (StuSubDetail enrollment : enrollments) {
//                try {
//                    if (enrollment.getSubject() != null) {
//                        aiAnalysisResultService.analyzeStudent(
//                                studentId,
//                                enrollment.getSubjectId(),
//                                enrollment.getSubject().getSubYear(),
//                                enrollment.getSubject().getSemester()
//                        );
//                        successCount++;
//                    }
//                } catch (Exception e) {
//                    System.err.println("⚠️ 과목 " + enrollment.getSubjectId() + " AI 분석 실패: " + e.getMessage());
//                }
//            }
//
//            System.out.println("✅ 등록금 납부 후 AI 분석 완료: " + successCount + "개 과목");
//
//        } catch (Exception e) {
//            System.err.println("⚠️ AI 분석 실패 (등록금 처리는 정상): " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    /**
     * 학생 → 학과 → 단과대 → CollTuit 를 통해 등록금 금액 조회
     */
    @Transactional(readOnly = true)
    protected Integer resolveTuitionAmountByStudent(Integer studentId) {
        Student student = studentJpaRepository.findById(studentId)
                .orElseThrow(() -> new CustomRestfullException(
                        "학생 정보를 찾을 수 없습니다.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        // 실제 엔티티 구조에 맞게 필드명만 맞춰주면 됨
        Integer collegeId = student.getDepartment()
                .getCollege()
                .getId();

        return collTuitJpaRepository.findAmountByCollegeId(collegeId);
    }


}
