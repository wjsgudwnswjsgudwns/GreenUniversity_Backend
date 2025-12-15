package com.green.university.service;

import com.green.university.repository.ProfessorJpaRepository;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.model.Department;
import com.green.university.repository.model.Professor;
import com.green.university.repository.model.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvisorAssignmentService {

    private final StudentJpaRepository studentRepository;
    private final ProfessorJpaRepository professorRepository;

    /**
     * 🔥 전체 학생에게 담당 교수 일괄 배정
     *
     * - 이미 담당 교수가 있는 학생은 건너뜁니다
     * - 학과별로 교수들에게 균등하게 배정합니다
     *
     * @return 배정 결과 통계
     */
    @Transactional
    public Map<String, Object> assignAdvisorToAllStudents() {
        // 담당 교수가 없는 모든 학생 조회
        List<Student> studentsWithoutAdvisor = studentRepository.findByAdvisorIsNull();

        int totalStudents = studentsWithoutAdvisor.size();
        int assignedCount = 0;
        int alreadyAssignedCount = studentRepository.findAll().size() - totalStudents;

        // 학과별로 그룹화
        Map<Integer, List<Student>> studentsByDept = studentsWithoutAdvisor.stream()
                .collect(Collectors.groupingBy(Student::getDeptId));

        List<Map<String, Object>> departmentDetails = new ArrayList<>();

        // 각 학과별로 처리
        for (Map.Entry<Integer, List<Student>> entry : studentsByDept.entrySet()) {
            Integer deptId = entry.getKey();
            List<Student> students = entry.getValue();

            List<Professor> professors = professorRepository.findByDepartmentId(deptId);

            if (professors.isEmpty()) {
                // 교수가 없는 학과는 건너뛰기
                Map<String, Object> deptDetail = new HashMap<>();
                deptDetail.put("deptId", deptId);
                deptDetail.put("studentCount", students.size());
                deptDetail.put("professorCount", 0);
                deptDetail.put("assigned", 0);
                deptDetail.put("message", "배정 가능한 교수가 없습니다");
                departmentDetails.add(deptDetail);
                continue;
            }

            // 라운드 로빈 방식으로 균등 배정
            int professorIndex = 0;
            for (Student student : students) {
                student.setAdvisor(professors.get(professorIndex));
                professorIndex = (professorIndex + 1) % professors.size();
                assignedCount++;
            }

            studentRepository.saveAll(students);

            Map<String, Object> deptDetail = new HashMap<>();
            deptDetail.put("deptId", deptId);
            deptDetail.put("deptName", students.get(0).getDepartment().getName());
            deptDetail.put("studentCount", students.size());
            deptDetail.put("professorCount", professors.size());
            deptDetail.put("assigned", students.size());
            deptDetail.put("avgPerProfessor", (double) students.size() / professors.size());
            departmentDetails.add(deptDetail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalStudents", totalStudents);
        result.put("assignedCount", assignedCount);
        result.put("alreadyAssignedCount", alreadyAssignedCount);
        result.put("departmentDetails", departmentDetails);

        return result;
    }

    /**
     * 특정 학생에게 담당 교수를 자동 배정합니다.
     *
     * @param studentId 학생 ID
     * @return 배정된 교수 정보
     */
    @Transactional
    public Professor assignAdvisorToStudent(Integer studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. ID: " + studentId));

        List<Professor> professors = professorRepository.findByDepartmentId(student.getDeptId());

        if (professors.isEmpty()) {
            throw new IllegalArgumentException("배정 가능한 교수가 없습니다. 학과 ID: " + student.getDeptId());
        }

        Professor selectedProfessor = findProfessorWithLeastStudents(professors);

        student.setAdvisor(selectedProfessor);
        studentRepository.save(student);

        return selectedProfessor;
    }

    /**
     * 특정 학과의 모든 학생에게 담당 교수를 자동 배정합니다.
     *
     * @param deptId 학과 ID
     * @return 배정된 학생 수
     */
    @Transactional
    public int assignAdvisorsToDepartment(Integer deptId) {
        List<Student> students = studentRepository.findByDeptIdAndAdvisorIsNull(deptId);

        if (students.isEmpty()) {
            return 0;
        }

        List<Professor> professors = professorRepository.findByDepartmentId(deptId);

        if (professors.isEmpty()) {
            throw new IllegalArgumentException("배정 가능한 교수가 없습니다. 학과 ID: " + deptId);
        }

        // 라운드 로빈 방식으로 균등하게 배정
        int professorIndex = 0;
        for (Student student : students) {
            student.setAdvisor(professors.get(professorIndex));
            professorIndex = (professorIndex + 1) % professors.size();
        }

        studentRepository.saveAll(students);
        return students.size();
    }

    /**
     * 담당 학생 수가 가장 적은 교수를 찾습니다.
     */
    private Professor findProfessorWithLeastStudents(List<Professor> professors) {
        Professor selectedProfessor = null;
        long minStudentCount = Long.MAX_VALUE;

        for (Professor professor : professors) {
            long studentCount = studentRepository.countByAdvisorId(professor.getId());

            if (studentCount < minStudentCount) {
                minStudentCount = studentCount;
                selectedProfessor = professor;
            }
        }

        return selectedProfessor;
    }

    /**
     * 특정 교수의 담당 학생 수를 조회합니다.
     */
    public long getAdviseeCount(Integer professorId) {
        return studentRepository.countByAdvisorId(professorId);
    }

    /**
     * 특정 교수의 담당 학생 목록을 조회합니다.
     */
    public List<Student> getAdviseeList(Integer professorId) {
        return studentRepository.findByAdvisorId(professorId);
    }

    /**
     * 학생의 담당 교수를 변경합니다.
     */
    @Transactional
    public Student changeAdvisor(Integer studentId, Integer professorId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. ID: " + studentId));

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new IllegalArgumentException("교수를 찾을 수 없습니다. ID: " + professorId));

        // 같은 학과인지 확인
        if (!student.getDeptId().equals(professor.getDeptId())) {
            throw new IllegalArgumentException("학생과 교수의 학과가 일치하지 않습니다.");
        }

        student.setAdvisor(professor);
        return studentRepository.save(student);
    }

    /**
     * 학과별 담당 교수 배정 현황 조회
     */
    public Map<String, Object> getDepartmentAdvisorStatus(Integer deptId) {
        List<Professor> professors = professorRepository.findByDepartmentId(deptId);
        List<Student> allStudents = studentRepository.findByDeptId(deptId);
        List<Student> assignedStudents = studentRepository.findByDeptIdAndAdvisorIsNotNull(deptId);

        List<Map<String, Object>> professorStats = new ArrayList<>();
        for (Professor professor : professors) {
            long count = studentRepository.countByAdvisorId(professor.getId());
            Map<String, Object> stat = new HashMap<>();
            stat.put("professorId", professor.getId());
            stat.put("professorName", professor.getName());
            stat.put("adviseeCount", count);
            professorStats.add(stat);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deptId", deptId);
        result.put("totalStudents", allStudents.size());
        result.put("assignedStudents", assignedStudents.size());
        result.put("unassignedStudents", allStudents.size() - assignedStudents.size());
        result.put("totalProfessors", professors.size());
        result.put("professorStats", professorStats);

        return result;
    }

    /**
     * 전체 학과 담당 교수 배정 현황 조회
     */
    public List<Map<String, Object>> getAllAdvisorStatus() {
        List<Department> departments = studentRepository.findAll().stream()
                .map(Student::getDepartment)
                .distinct()
                .collect(Collectors.toList());

        List<Map<String, Object>> statusList = new ArrayList<>();

        for (Department dept : departments) {
            statusList.add(getDepartmentAdvisorStatus(dept.getId()));
        }

        return statusList;
    }
}