package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.BreakAppFormDto;
import com.green.university.handler.exception.CustomPathException;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.BreakAppJpaRepository;
import com.green.university.repository.StudentJpaRepository;
import java.util.stream.Collectors;
import com.green.university.repository.model.BreakApp;

/**
 * @author 서영
 *
 */

@Service
public class BreakAppService {

    @Autowired
    private BreakAppJpaRepository breakAppJpaRepository;

    @Autowired
    private StudentJpaRepository studentJpaRepository;

	@Autowired
	private StuStatService stuStatService;

	/**
	 * @param breakAppFormDto 휴학 신청
	 */
	@Transactional
    public void createBreakApp(BreakAppFormDto breakAppFormDto) {
        // 이미 처리중인 휴학 신청 내역이 있다면 신청 불가능
        List<BreakApp> existingList = readByStudentId(breakAppFormDto.getStudentId());
        for (BreakApp b : existingList) {
            if ("처리중".equals(b.getStatus())) {
                throw new CustomPathException("이미 처리중인 신청 내역이 존재합니다.", HttpStatus.BAD_REQUEST,
                        "/break/appList");
            }
        }
        // 새로운 휴학 신청 엔티티 생성
        BreakApp newApp = new BreakApp();
        // 학생 설정: studentId 필드와 연관관계 모두 채움
        Integer stuId = breakAppFormDto.getStudentId();
        if (stuId != null) {
            newApp.setStudent(studentJpaRepository.findById(stuId)
                    .orElseThrow(() -> new CustomRestfullException("학생 정보를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST)));
        }
        newApp.setStudentGrade(breakAppFormDto.getStudentGrade());
        newApp.setFromYear(breakAppFormDto.getFromYear());
        newApp.setFromSemester(breakAppFormDto.getFromSemester());
        newApp.setToYear(breakAppFormDto.getToYear());
        newApp.setToSemester(breakAppFormDto.getToSemester());
        newApp.setType(breakAppFormDto.getType());
        // 기본 상태를 처리중으로 설정
        newApp.setStatus("처리중");
        // 신청 날짜는 현재 날짜로 설정
        newApp.setAppDate(new java.sql.Date(System.currentTimeMillis()));
        // 저장
        breakAppJpaRepository.save(newApp);
    }

	/**
	 * @param studentId 해당 학생의 휴학 신청 내역 조회
	 */
	@Transactional
    public List<BreakApp> readByStudentId(Integer studentId) {
        // find all by student id via JPA. If no custom method, filter in memory
        return breakAppJpaRepository.findAll().stream()
                .filter(b -> b.getStudentId() != null && b.getStudentId().equals(studentId))
                .collect(Collectors.toList());
    }

	/**
	 * @param status 처리하지 않은 휴학 신청 내역 조회 (교직원용)
	 */
	@Transactional
    public List<BreakApp> readByStatus(String status) {
        return breakAppJpaRepository.findAll().stream()
                .filter(b -> b.getStatus() != null && b.getStatus().equals(status))
                .collect(Collectors.toList());
    }

	/**
	 * @param id 특정 휴학 신청서 조회
	 */
	@Transactional
    public BreakApp readById(Integer id) {
        return breakAppJpaRepository.findById(id)
                .orElseThrow(() -> new CustomRestfullException("휴학 신청 내역을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    }

	/**
	 * 아직 처리되지 않은 휴학 신청 취소 (학생)
	 */
	@Transactional
    public void deleteById(Integer id) {
        // 처리중 상태인지 확인
        BreakApp breakAppEntity = readById(id);
        if (!"처리중".equals(breakAppEntity.getStatus())) {
            throw new CustomRestfullException("이미 처리가 완료되어, 신청이 취소되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }
        breakAppJpaRepository.deleteById(id);
    }

	/**
	 * 휴학 신청 처리 (교직원)
	 */
	@Transactional
    public void updateById(Integer id, String status) {
        BreakApp breakAppEntity = readById(id);
        breakAppEntity.setStatus(status);
        breakAppJpaRepository.save(breakAppEntity);
        // 승인 시 학적 상태를 휴학으로 변경하기
        if ("승인".equals(status)) {
            String newToDate;
            if (breakAppEntity.getToSemester() == 1) {
                newToDate = breakAppEntity.getToYear() + "-08-31";
            } else {
                newToDate = (breakAppEntity.getToYear() + 1) + "-02-28";
            }
            stuStatService.updateStatus(breakAppEntity.getStudentId(), "휴학", newToDate, id);
        }
    }

}
