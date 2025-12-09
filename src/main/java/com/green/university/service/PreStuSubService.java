package com.green.university.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.model.PreStuSubId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.response.StuSubAppDto;
import com.green.university.dto.response.StuSubDayTimeDto;
import com.green.university.dto.response.StuSubSumGradesDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.PreStuSubJpaRepository;
import com.green.university.repository.model.PreStuSub;
import com.green.university.repository.model.Subject;
import com.green.university.utils.Define;
import com.green.university.utils.StuSubUtil;

/**
 * @author 서영
 *
 */
@Service
public class PreStuSubService {

    @Autowired
    private PreStuSubJpaRepository preStuSubJpaRepository;

	@Autowired
	private SubjectService subjectService;

    @Autowired
    private SubjectJpaRepository subjectJpaRepository;



    // 학생의 예비 수강신청 내역에 해당 강의가 존재하는지 확인(JPA)
    public PreStuSub readPreStuSub(Integer studentId, Integer subjectId) {
        return preStuSubJpaRepository.findByIdStudentIdAndIdSubjectId(studentId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<PreStuSub> readPreStuSubList(Integer studentId) {

        List<PreStuSub> allPreStuSubs = preStuSubJpaRepository.findByIdStudentId(studentId);

        // 현재 학기만 필터링
        return allPreStuSubs.stream()
                .filter(ps -> {
                    Subject subject = subjectJpaRepository.findById(ps.getSubjectId()).orElse(null);
                    return subject != null
                            && subject.getSubYear().equals(Define.CURRENT_YEAR)
                            && subject.getSemester().equals(Define.CURRENT_SEMESTER);
                })
                .collect(Collectors.toList());
    }

	// 학생의 예비 수강신청 내역 추가
    @Transactional
    public void createPreStuSub(Integer studentId, Integer subjectId) {
        // 신청 대상 과목 정보
        Subject targetSubject = subjectJpaRepository.findById(subjectId)
                .orElseThrow(() -> new CustomRestfullException("과목 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 중복 체크
        Optional<PreStuSub> existing = preStuSubJpaRepository.findById(new PreStuSubId(studentId, subjectId));
        if (existing.isPresent()) {
            throw new CustomRestfullException("이미 예비 수강 신청한 과목입니다.", HttpStatus.BAD_REQUEST);
        }

        // ✅ 정원 체크 (preNumOfStudent 사용)
        if (targetSubject.getPreNumOfStudent() >= targetSubject.getCapacity()) {
            throw new CustomRestfullException("정원이 초과되었습니다.", HttpStatus.BAD_REQUEST);
        }

        // 학점 체크
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
        StuSubUtil.checkSumGrades(targetSubject, stuSubSumGradesDto);

        // 시간표 겹침 체크
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
        StuSubUtil.checkDayTime(targetSubject, dayTimeList);

        // 예비 수강 신청 내역 추가
        PreStuSub preStuSub = new PreStuSub(studentId, subjectId);
        preStuSubJpaRepository.save(preStuSub);

        // ✅ 예비 수강 신청 현재인원 +1
        subjectService.updatePlusPreNumOfStudent(subjectId);
    }

    // 학생의 예비 수강신청 내역 삭제(JPA)
    @Transactional
    public void deletePreStuSub(Integer studentId, Integer subjectId) {
        PreStuSub preStuSub = preStuSubJpaRepository.findById(new PreStuSubId(studentId, subjectId))
                .orElseThrow(() -> new CustomRestfullException("예비 수강신청 취소가 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        preStuSubJpaRepository.delete(preStuSub);

        // ✅ 예비 수강 신청 현재인원 -1
        subjectService.updateMinusPreNumOfStudent(subjectId);
    }
}
