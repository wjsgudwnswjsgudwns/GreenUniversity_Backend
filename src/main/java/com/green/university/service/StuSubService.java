package com.green.university.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.green.university.repository.*;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.response.StuSubDayTimeDto;
import com.green.university.dto.response.StuSubSumGradesDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.utils.Define;
import com.green.university.utils.StuSubUtil;

/**
 * @author 서영
 *
 */
@Service
public class StuSubService {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private StuSubJpaRepository stuSubJpaRepository;

    @Autowired
    private SubjectJpaRepository subjectJpaRepository;

    @Autowired
    private PreStuSubJpaRepository preStuSubJpaRepository;

    @Autowired
    private StuSubDetailJpaRepository stuSubDetailJpaRepository;

    @Autowired
    private StudentJpaRepository studentJpaRepository;

    @Autowired
    private PreStuSubService preStuSubService;

    // 학생의 수강신청 내역에 해당 강의가 존재하는지 확인
    @Transactional(readOnly = true)
    public StuSub readStuSub(Integer studentId, Integer subjectId) {

        return stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElse(null);
    }

    // 학생의 해당 학기 수강신청 내역 조회
    @Transactional(readOnly = true)
    public List<StuSub> readStuSubList(Integer studentId) {

        return stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId,
                Define.CURRENT_YEAR,
                Define.CURRENT_SEMESTER
        );
    }

    // StuSubService.java의 createStuSub 메서드 수정

    @Transactional
    public void createStuSub(Integer studentId, Integer subjectId) {

        // 신청 대상 과목 정보
        Subject targetSubject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 기존 수강 신청 내역 확인
        Optional<StuSub> existingStuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId);

        // 본 수강 신청 정원 체크 로직
        boolean isOverCapacity = targetSubject.getPreNumOfStudent() > targetSubject.getCapacity();

        if (isOverCapacity) {
            // 정원 초과 과목: 예비 신청(PRE) 여부 확인
            if (!existingStuSub.isPresent() || !"PRE".equals(existingStuSub.get().getEnrollmentType())) {
                throw new CustomRestfullException("예비 수강 신청을 하지 않은 과목입니다. 정원 초과 과목은 예비 수강 신청자만 신청 가능합니다.", HttpStatus.BAD_REQUEST);
            }

            // 현재 인원이 정원을 초과했는지만 체크 (선착순)
            if (targetSubject.getNumOfStudent() >= targetSubject.getCapacity()) {
                throw new CustomRestfullException("수강 정원이 초과되었습니다.", HttpStatus.BAD_REQUEST);
            }

            // ✅ 예비 수강신청(PRE)을 정식 수강신청(ENROLLED)으로 변경
            StuSub preStuSub = existingStuSub.get();
            preStuSub.setEnrollmentType("ENROLLED");
            stuSubJpaRepository.save(preStuSub);

            // 해당 강의 현재인원 +1
            subjectService.updatePlusNumOfStudent(subjectId);

            return; // 변경 완료, 메서드 종료
        }

        // === 정원 미만 과목 처리 ===

        // 정원 체크
        if (targetSubject.getNumOfStudent() >= targetSubject.getCapacity()) {
            throw new CustomRestfullException("수강 정원이 초과되었습니다.", HttpStatus.BAD_REQUEST);
        }

        // 중복 신청 체크
        if (existingStuSub.isPresent()) {
            throw new CustomRestfullException("이미 수강 신청한 과목입니다.", HttpStatus.BAD_REQUEST);
        }

        // 본 수강 신청 완료된 과목들의 총 학점 계산 (ENROLLED만)
        List<StuSub> completedStuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId, Define.CURRENT_YEAR, Define.CURRENT_SEMESTER);

        int sumGrades = completedStuSubs.stream()
                .filter(ss -> "ENROLLED".equals(ss.getEnrollmentType()) && ss.getSubject() != null)
                .mapToInt(ss -> ss.getSubject().getGrades())
                .sum();

        StuSubSumGradesDto stuSubSumGradesDto = new StuSubSumGradesDto();
        stuSubSumGradesDto.setSumGrades(sumGrades);

        // 최대 수강 가능 학점을 넘지 않는지 확인
        StuSubUtil.checkSumGrades(targetSubject, stuSubSumGradesDto);

        // 본 수강 신청 완료된 과목들의 시간표로 겹침 체크 (ENROLLED만)
        List<StuSubDayTimeDto> dayTimeList = completedStuSubs.stream()
                .filter(ss -> "ENROLLED".equals(ss.getEnrollmentType()) && ss.getSubject() != null)
                .map(ss -> {
                    Subject subject = ss.getSubject();
                    StuSubDayTimeDto dto = new StuSubDayTimeDto();
                    dto.setSubDay(subject.getSubDay());
                    dto.setStartTime(subject.getStartTime());
                    dto.setEndTime(subject.getEndTime());
                    return dto;
                })
                .collect(Collectors.toList());

        // 현재 학생의 시간표와 겹치지 않는지 확인
        StuSubUtil.checkDayTime(targetSubject, dayTimeList);

        // 수강신청 내역 추가
        Student student = studentJpaRepository.findById(studentId)
                .orElseThrow(() -> new CustomRestfullException("학생 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        StuSub stuSub = new StuSub();
        stuSub.setStudent(student);
        stuSub.setSubject(targetSubject);
        stuSub.setEnrollmentType("ENROLLED"); // ✅ 정식 수강신청

        StuSub savedStuSub = stuSubJpaRepository.save(stuSub);

        // 수강 상세 내역 추가
        StuSubDetail stuSubDetail = new StuSubDetail();
        stuSubDetail.setStuSub(savedStuSub);
        stuSubDetail.setStudentId(studentId);
        stuSubDetail.setSubjectId(subjectId);

        stuSubDetailJpaRepository.save(stuSubDetail);

        // 해당 강의 현재인원 +1
        subjectService.updatePlusNumOfStudent(subjectId);
    }

    // 학생의 수강신청 내역 삭제
    @Transactional
    public void deleteStuSub(Integer studentId, Integer subjectId) {

        // 수강신청 내역 삭제
        StuSub stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElseThrow(() -> new CustomRestfullException("예비 수강신청 취소가 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        stuSubJpaRepository.delete(stuSub);

        // 해당 강의 현재인원 -1
        subjectService.updateMinusNumOfStudent(subjectId);
    }

    // 예비 수강 신청 기간 -> 수강 신청 기간 변경 시 로직 (개선)
    @Transactional
    public void createStuSubByPreStuSub() {
        System.out.println("=== 예비 수강 신청 → 본 수강 신청 자동 전환 시작 ===");

        // 1. 정원 >= 예비신청인원인 강의 (자동 승인)
        List<Subject> approvedSubjects = subjectJpaRepository.findByCapacityGreaterThanEqualPreNumOfStudent(
                Define.CURRENT_YEAR,
                Define.CURRENT_SEMESTER
        );

        System.out.println("자동 승인 대상 과목 수: " + approvedSubjects.size());

        for (Subject subject : approvedSubjects) {
            // StuSub 테이블에서 enrollmentType="PRE"인 학생들 조회
            List<StuSub> preAppList = stuSubJpaRepository.findBySubjectIdAndEnrollmentType(
                    subject.getId(), "PRE");

            System.out.println("과목 [" + subject.getName() + "] - 예비 신청자: " + preAppList.size() + "명");

            for (StuSub preStuSub : preAppList) {
                try {
                    // enrollmentType을 PRE -> ENROLLED로 변경
                    preStuSub.setEnrollmentType("ENROLLED");
                    stuSubJpaRepository.save(preStuSub);

                    // 현재 인원 증가
                    subject.setNumOfStudent(subject.getNumOfStudent() + 1);

                    System.out.println("  → 학생 " + preStuSub.getStudentId() + " 자동 승인 완료 (PRE → ENROLLED)");
                } catch (Exception e) {
                    System.err.println(" 자동 처리 실패: 학생 " + preStuSub.getStudentId() +
                            ", 과목 " + preStuSub.getSubjectId() + " - " + e.getMessage());
                }
            }

            subjectJpaRepository.save(subject);
        }

        // 2. 정원 < 예비신청인원인 강의 (수동 신청 필요 - enrollmentType은 PRE로 유지)
        List<Subject> overCapacitySubjects = subjectJpaRepository.findByCapacityLessThanPreNumOfStudent(
                Define.CURRENT_YEAR,
                Define.CURRENT_SEMESTER
        );

        System.out.println("정원 초과 과목 수: " + overCapacitySubjects.size());

        for (Subject subject : overCapacitySubjects) {
            int preApplicants = subject.getPreNumOfStudent();
            int capacity = subject.getCapacity();

            System.out.println("과목 [" + subject.getName() + "] - 정원: " + capacity +
                    ", 예비 신청: " + preApplicants + " (정원 초과 " + (preApplicants - capacity) + "명)");

            // 정원 초과 과목은 numOfStudent를 0으로 유지 (자동 승인 안함)
            // enrollmentType은 "PRE"로 유지 (학생이 수동으로 신청 시 "ENROLLED"로 변경됨)
            subject.setNumOfStudent(0);
            subjectJpaRepository.save(subject);

            System.out.println("  → 수동 신청 필요 (enrollmentType은 PRE로 유지)");
        }

        System.out.println("=== 자동 전환 완료 ===");
    }

    // 수강 신청 내역과 예비 수강 신청 내역 조인 후 조회 -> 예비 수강 신청에만 존재
    // enrollmentType="PRE"인 것만 반환 (신청 미완료)
    @Transactional(readOnly = true)
    public List<PreStuSub> readPreStuSubByStuSub(Integer studentId) {
        // StuSub에서 enrollmentType="PRE"인 항목만 조회
        List<StuSub> preStuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId,
                Define.CURRENT_YEAR,
                Define.CURRENT_SEMESTER
        );

        // PRE 타입만 필터링하고 PreStuSub 객체로 변환
        List<PreStuSub> result = preStuSubs.stream()
                .filter(ss -> "PRE".equals(ss.getEnrollmentType()))
                .map(ss -> new PreStuSub(ss.getStudentId(), ss.getSubjectId()))
                .collect(Collectors.toList());

        return result;
    }

    // 점수 입력 시 F면 취득학점 0, F가 아니면 강의의 이수학점
    @Transactional
    public void updateCompleteGrade(Integer studentId, Integer subjectId, Integer completeGrade) {
        StuSub stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElseThrow(() -> new CustomRestfullException("수강 신청 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        stuSub.setCompleteGrade(completeGrade);
        stuSubJpaRepository.save(stuSub);
    }
}