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

import com.green.university.dto.response.StuSubAppDto;
import com.green.university.dto.response.StuSubDayTimeDto;
import com.green.university.dto.response.StuSubSumGradesDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.utils.Define;
import com.green.university.utils.StuSubUtil;

/**
 * @author 서영
 * 방법 1: 예비 수강신청도 StuSub 테이블에 enrollmentType="PRE"로 저장
 */
@Service
public class PreStuSubService {

    @Autowired
    private PreStuSubJpaRepository preStuSubJpaRepository;

    @Autowired
    private StuSubJpaRepository stuSubJpaRepository;

    @Autowired
    private StuSubDetailJpaRepository stuSubDetailJpaRepository;

    @Autowired
    private StudentJpaRepository studentJpaRepository;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private SubjectJpaRepository subjectJpaRepository;


    // 학생의 예비 수강신청 내역에 해당 강의가 존재하는지 확인
    // 이제 StuSub 테이블에서 enrollmentType="PRE"인 것을 조회
    public PreStuSub readPreStuSub(Integer studentId, Integer subjectId) {
        // 기존 PreStuSub 테이블도 확인 (하위 호환성)
        PreStuSub legacyPreStuSub = preStuSubJpaRepository.findByIdStudentIdAndIdSubjectId(studentId, subjectId);
        if (legacyPreStuSub != null) {
            return legacyPreStuSub;
        }

        // StuSub에서 PRE 타입 확인
        Optional<StuSub> stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId);
        if (stuSub.isPresent() && "PRE".equals(stuSub.get().getEnrollmentType())) {
            // PreStuSub 객체로 변환해서 반환 (호환성 유지)
            return new PreStuSub(studentId, subjectId);
        }

        return null;
    }

    @Transactional(readOnly = true)
    public List<PreStuSub> readPreStuSubList(Integer studentId) {
        // StuSub에서 enrollmentType="PRE"인 항목 조회
        List<StuSub> preStuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId,
                Define.getCurrentYear(),
                Define.getCurrentSemester()
        );

        // PRE 타입만 필터링하고 PreStuSub 객체로 변환
        return preStuSubs.stream()
                .filter(ss -> "PRE".equals(ss.getEnrollmentType()))
                .map(ss -> new PreStuSub(ss.getStudentId(), ss.getSubjectId()))
                .collect(Collectors.toList());
    }

    // 학생의 예비 수강신청 내역 추가
    @Transactional
    public void createPreStuSub(Integer studentId, Integer subjectId) {
        // 신청 대상 과목 정보
        Subject targetSubject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 중복 체크 (StuSub 테이블에서)
        Optional<StuSub> existingStuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId);
        if (existingStuSub.isPresent()) {
            throw new CustomRestfullException("이미 예비 수강 신청한 과목입니다.", HttpStatus.BAD_REQUEST);
        }

        // 학점 체크
        List<StuSub> preStuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId,
                Define.getCurrentYear(),
                Define.getCurrentSemester()
        );

        // PRE 타입만 필터링해서 학점 계산
        int sumGrades = preStuSubs.stream()
                .filter(ss -> "PRE".equals(ss.getEnrollmentType()) && ss.getSubject() != null)
                .mapToInt(ss -> ss.getSubject().getGrades())
                .sum();

        StuSubSumGradesDto stuSubSumGradesDto = new StuSubSumGradesDto();
        stuSubSumGradesDto.setSumGrades(sumGrades);
        StuSubUtil.checkSumGrades(targetSubject, stuSubSumGradesDto);

        // 시간표 겹침 체크
        List<StuSubDayTimeDto> dayTimeList = preStuSubs.stream()
                .filter(ss -> "PRE".equals(ss.getEnrollmentType()) && ss.getSubject() != null)
                .map(ss -> {
                    Subject subject = ss.getSubject();
                    StuSubDayTimeDto dto = new StuSubDayTimeDto();
                    dto.setSubDay(subject.getSubDay());
                    dto.setStartTime(subject.getStartTime());
                    dto.setEndTime(subject.getEndTime());
                    return dto;
                })
                .collect(Collectors.toList());
        StuSubUtil.checkDayTime(targetSubject, dayTimeList);

        // 예비 수강 신청 내역 추가 (StuSub 테이블에 enrollmentType="PRE"로 저장)
        Student student = studentJpaRepository.findById(studentId)
                .orElseThrow(() -> new CustomRestfullException("학생 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        StuSub stuSub = new StuSub();
        stuSub.setStudent(student);
        stuSub.setSubject(targetSubject);
        stuSub.setEnrollmentType("PRE"); // ✅ 예비 수강신청

        StuSub savedStuSub = stuSubJpaRepository.save(stuSub);

        // 수강 상세 내역 추가
        StuSubDetail stuSubDetail = new StuSubDetail();
        stuSubDetail.setStuSub(savedStuSub);
        stuSubDetail.setStudentId(studentId);
        stuSubDetail.setSubjectId(subjectId);

        stuSubDetailJpaRepository.save(stuSubDetail);

        // 예비 수강 신청 현재인원 +1
        subjectService.updatePlusPreNumOfStudent(subjectId);
    }

    // 학생의 예비 수강신청 내역 삭제
    @Transactional
    public void deletePreStuSub(Integer studentId, Integer subjectId) {
        // StuSub에서 PRE 타입 찾기
        StuSub stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId)
                .orElseThrow(() -> new CustomRestfullException("예비 수강신청 취소가 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        // PRE 타입이 아니면 에러
        if (!"PRE".equals(stuSub.getEnrollmentType())) {
            throw new CustomRestfullException("예비 수강신청 내역이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        stuSubJpaRepository.delete(stuSub);

        // 예비 수강 신청 현재인원 -1
        subjectService.updateMinusPreNumOfStudent(subjectId);
    }
}