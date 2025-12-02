package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.handler.exception.CustomRestfullException;
import java.util.stream.Collectors;
import com.green.university.repository.StuStatJpaRepository;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.BreakAppJpaRepository;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.BreakApp;
import java.util.Comparator;
import com.green.university.repository.model.StuStat;

/**
 * @author 서영
 *
 */
@Service
public class StuStatService {

    @Autowired
    private StuStatJpaRepository stuStatJpaRepository;

    @Autowired
    private StudentJpaRepository studentJpaRepository;

    @Autowired
    private BreakAppJpaRepository breakAppJpaRepository;

	/**
	 * @param studentId
	 * @return 해당 학생의 현재 학적 상태 (.getStatus())
	 */
	@Transactional
	public StuStat readCurrentStatus(Integer studentId) {
        // JPA: find all statuses for the student and return the one with the largest ID
        return stuStatJpaRepository.findAll().stream()
                .filter(s -> s.getStudentId() != null && s.getStudentId().equals(studentId))
                .max(Comparator.comparing(StuStat::getId))
                .orElse(null);
	}

	/**
	 * @param studentId
	 * @return 해당 학생의 전체 학적 변동 내역 조회
	 */
	@Transactional
	public List<StuStat> readStatusList(Integer studentId) {
        return stuStatJpaRepository.findAll().stream()
                .filter(s -> s.getStudentId() != null && s.getStudentId().equals(studentId))
                .sorted(Comparator.comparing(StuStat::getId).reversed())
                .collect(java.util.stream.Collectors.toList());
	}

	/**
	 * 모든 학생 id 리스트
	 */
	public List<Integer> readIdList() {
        return studentJpaRepository.findAll().stream()
                .map(Student::getId)
                .collect(java.util.stream.Collectors.toList());
	}

	/*
	 * 처음 학생이 생성될 때 학적 상태 지정 (재학)
	 *
	 * 첫 학적 상태 저장과 이후 변동 사항을 저장할 때의 메서드를 분리한 이유는 이후 변동 사항을 지정할 때에는 기존의 상태 데이터의
	 * toDate를 현재 날짜로 바꿔주는 작업이 추가로 필요하기 때문임
	 */
	@Transactional
	public void createFirstStatus(Integer studentId) {
        // Create initial status entity with status "재학" and to_date far future
        StuStat stuStat = new StuStat();
        // Set student reference
        Student student = studentJpaRepository.findById(studentId).orElse(null);
        if (student != null) {
            stuStat.setStudent(student);
        }
        stuStat.setStatus("재학");
        // fromDate = now
        stuStat.setFromDate(new java.sql.Date(System.currentTimeMillis()));
        // toDate = 9999-01-01
        stuStat.setToDate(java.sql.Date.valueOf("9999-01-01"));
        // breakApp remains null
        stuStatJpaRepository.save(stuStat);
	}

	/**
	 * 학적 상태 변동 새로운 상태 추가 + 기존 학적 상태의 to_date를 now()로 변경 breakAppId가 없다면 null로 받기
	 */

	public void updateStatus(Integer studentId, String newStatus, String newToDate, Integer breakAppId) {
        // Find the most recent status entity and update its toDate to now
        StuStat current = readCurrentStatus(studentId);
        if (current != null) {
            current.setToDate(new java.sql.Date(System.currentTimeMillis()));
            stuStatJpaRepository.save(current);
        }
        // Create a new status entity
        StuStat newStatusEntity = new StuStat();
        Student student = studentJpaRepository.findById(studentId).orElse(null);
        if (student != null) {
            newStatusEntity.setStudent(student);
        }
        newStatusEntity.setStatus(newStatus);
        // fromDate = now
        newStatusEntity.setFromDate(new java.sql.Date(System.currentTimeMillis()));
        // toDate from parameter
        if (newToDate != null) {
            newStatusEntity.setToDate(java.sql.Date.valueOf(newToDate));
        }
        // set breakApp if provided
        if (breakAppId != null) {
            BreakApp breakApp = breakAppJpaRepository.findById(breakAppId).orElse(null);
            newStatusEntity.setBreakApp(breakApp);
        }
        stuStatJpaRepository.save(newStatusEntity);
	}

}
