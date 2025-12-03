package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.StudentListForm;
import com.green.university.repository.StudentJpaRepository;
import java.util.stream.Collectors;
import java.util.Comparator;
import com.green.university.repository.model.Student;

/**
 * 학생 관련 서비스
 * 
 * @author 김지현
 *
 */
@Service
public class StudentService {

    @Autowired
    private StudentJpaRepository studentJpaRepository;

	/**
	 * 
	 * @param studentListForm
	 * @return 학생 리스트
	 */
    @Transactional(readOnly = true)
    public Page<Student> readStudentList(StudentListForm studentListForm) {
        int pageSize = 20;
        int page = (studentListForm.getPage() == null || studentListForm.getPage() < 0)
                ? 0 : studentListForm.getPage();

        Pageable pageable = PageRequest.of(page, pageSize);

        // studentId로 조회하는 경우
        if (studentListForm.getStudentId() != null) {
            return studentJpaRepository.findById(studentListForm.getStudentId(), pageable);
        }

        // deptId로 조회하는 경우
        if (studentListForm.getDeptId() != null) {
            return studentJpaRepository.findByDepartmentId(studentListForm.getDeptId(), pageable);
        }

        // 전체 조회
        return studentJpaRepository.findAll(pageable);
    }

	/**
	 * 
	 * @param studentListForm
	 * @return 학생 수
	 */
    @Transactional(readOnly = true)
    public long readStudentAmount(StudentListForm studentListForm) {
        // studentId로 조회하는 경우
        if (studentListForm.getStudentId() != null) {
            return studentJpaRepository.existsById(studentListForm.getStudentId()) ? 1 : 0;
        }

        // deptId로 조회하는 경우
        if (studentListForm.getDeptId() != null) {
            return studentJpaRepository.countByDepartmentId(studentListForm.getDeptId());
        }

        // 전체 학생 수
        return studentJpaRepository.count();
    }

	/**
	 * 학생 학년과 학기 업데이트
	 */
	@Transactional
    public void updateStudentGradeAndSemester() {
        // In the original MyBatis implementation, multiple update queries advanced the students' grade and semester.
        // Here, we replicate the logic by iterating over all students and updating their fields accordingly.
        List<Student> students = studentJpaRepository.findAll();
        for (Student s : students) {
            Integer grade = s.getGrade();
            Integer semester = s.getSemester();
            if (grade == null || semester == null) {
                // if grade or semester is null, skip updating
                continue;
            }
            // Advance semester and grade similar to the original logic
            if (grade == 1 && semester == 2) {
                s.setGrade(2);
                s.setSemester(1);
            } else if (grade == 2 && semester == 1) {
                s.setSemester(2);
            } else if (grade == 2 && semester == 2) {
                s.setGrade(3);
                s.setSemester(1);
            } else if (grade == 3 && semester == 1) {
                s.setSemester(2);
            } else if (grade == 3 && semester == 2) {
                s.setGrade(4);
                s.setSemester(1);
            } else if (grade == 4 && semester == 1) {
                s.setSemester(2);
            } else if (grade == 4 && semester == 2) {
                // At the end of 4th year 2nd semester, keep grade/semester unchanged
                // (or optionally set to graduation)
            }
        }
        // Save all updated students
        studentJpaRepository.saveAll(students);
    }

}
