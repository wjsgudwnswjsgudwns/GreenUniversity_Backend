package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
	@Transactional
    public List<Student> readStudentList(StudentListForm studentListForm) {
        // Fetch all students via JPA and then filter/paginate based on the form.
        List<Student> all = studentJpaRepository.findAll();

        // Filter by student ID if present
        if (studentListForm.getStudentId() != null) {
            return all.stream()
                    .filter(s -> s.getId().equals(studentListForm.getStudentId()))
                    .collect(Collectors.toList());
        }

        // Filter by department ID if present
        if (studentListForm.getDeptId() != null) {
            return all.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(studentListForm.getDeptId()))
                    .collect(Collectors.toList());
        }

        // If page is provided, perform simple pagination (20 per page)
        Integer rawPage = studentListForm.getPage();
        int page = (rawPage == null || rawPage < 1) ? 1 : rawPage;

        int pageSize = 20;
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, all.size());

        if (fromIndex >= all.size()) {
            return List.of();
        }
        return all.subList(fromIndex, toIndex);
    }

	/**
	 * 
	 * @param studentListForm
	 * @return 학생 수
	 */
	@Transactional
    public Integer readStudentAmount(StudentListForm studentListForm) {
        List<Student> all = studentJpaRepository.findAll();
        if (studentListForm.getDeptId() != null) {
            return (int) all.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().getId().equals(studentListForm.getDeptId()))
                    .count();
        }
        return all.size();
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
