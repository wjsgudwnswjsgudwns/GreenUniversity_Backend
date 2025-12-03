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

    @Transactional
    public void createStuSub(Integer studentId, Integer subjectId) {

        // 신청 대상 과목 정보
        Subject targetSubject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 현재 총 신청 학점 - PreStuSub에서 Subject 조회
        List<PreStuSub> preStuSubs = preStuSubJpaRepository.findByIdStudentId(studentId);

        int sumGrades = preStuSubs.stream()
                .map(ps -> subjectJpaRepository.findById(ps.getSubjectId()).orElse(null))
                .filter(subject -> subject != null
                        && subject.getSubYear().equals(Define.CURRENT_YEAR)
                        && subject.getSemester().equals(Define.CURRENT_SEMESTER))
                .mapToInt(Subject::getGrades)
                .sum();

        StuSubSumGradesDto stuSubSumGradesDto = new StuSubSumGradesDto();
        stuSubSumGradesDto.setSumGrades(sumGrades);

        // 최대 수강 가능 학점을 넘지 않는지 확인
        StuSubUtil.checkSumGrades(targetSubject, stuSubSumGradesDto);

        // 해당 학생의 예비 수강 신청 내역 시간표
        List<StuSubDayTimeDto> dayTimeList = preStuSubs.stream()
                .map(ps -> subjectJpaRepository.findById(ps.getSubjectId()).orElse(null))
                .filter(subject -> subject != null)
                .map(subject -> {
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
        PreStuSub preStuSub = new PreStuSub(studentId, subjectId);
        preStuSubJpaRepository.save(preStuSub);

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

	// 예비 수강 신청 기간 -> 수강 신청 기간 변경 시 로직
    @Transactional
    public void createStuSubByPreStuSub() {

        // 1. 정원 >= 신청인원인 강의
        List<Subject> subjects1 = subjectJpaRepository.findByCapacityGreaterThanEqualNumOfStudent();

        for (Subject subject : subjects1) {

            // 예비 수강 신청에서 해당 강의를 신청했던 내역 가져오기
            List<PreStuSub> preAppList = preStuSubJpaRepository.findByIdSubjectId(subject.getId());

            // 예비 수강 신청했던 인원들이 자동으로 수강 신청되도록
            // 해당 내역 그대로 수강 신청 추가
            for (PreStuSub pss : preAppList) {
                // 수강 신청 내역이 없다면
                Optional<StuSub> existingStuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(
                        pss.getStudentId(), pss.getSubjectId());

                if (!existingStuSub.isPresent()) {
                    // 수강신청 내역 추가
                    StuSub stuSub = new StuSub();
                    Student student = studentJpaRepository.findById(pss.getStudentId())
                            .orElseThrow(() -> new CustomRestfullException("학생 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
                    stuSub.setStudent(student);
                    stuSub.setSubject(subject);

                    StuSub savedStuSub = stuSubJpaRepository.save(stuSub);

                    // 수강 상세 내역에도 데이터 추가
                    StuSubDetail stuSubDetail = new StuSubDetail();
                    stuSubDetail.setId(savedStuSub.getId());
                    stuSubDetail.setStudentId(pss.getStudentId());
                    stuSubDetail.setSubjectId(pss.getSubjectId());
                    stuSubDetailJpaRepository.save(stuSubDetail);
                }
            }
        }

        // 2. 정원 < 신청인원인 강의
        List<Subject> subjects2 = subjectJpaRepository.findByCapacityLessThanNumOfStudent();

        for (Subject subject : subjects2) {
            // 강의의 현재 인원 초기화
            subject.setNumOfStudent(0);
            subjectJpaRepository.save(subject);
        }
    }

    // 수강 신청 내역과 예비 수강 신청 내역 조인 후 조회 -> 예비 수강 신청에만 존재
    @Transactional(readOnly = true)
    public List<PreStuSub> readPreStuSubByStuSub(Integer studentId) {
        // 예비 수강 신청 목록
        List<PreStuSub> preStuSubs = preStuSubJpaRepository.findByIdStudentId(studentId);

        // 수강 신청에 없는 것만 필터링
        List<PreStuSub> result = preStuSubs.stream()
                .filter(pss -> {
                    Optional<StuSub> stuSub = stuSubJpaRepository.findByStudentIdAndSubjectId(
                            pss.getStudentId(), pss.getSubjectId());
                    return !stuSub.isPresent(); // 수강 신청에 없는 것만
                })
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
