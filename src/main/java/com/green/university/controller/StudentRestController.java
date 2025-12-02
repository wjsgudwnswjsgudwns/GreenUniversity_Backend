package com.green.university.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.model.Student;

/**
 * REST API controller for managing students.
 *
 * 기존의 학생 관련 기능은 MyBatis 기반 서비스와 컨트롤러에 의존하고 있으나,
 * JSON 기반 API로 점진적으로 이전하기 위해 이 컨트롤러를 추가했습니다.
 * 엔티티 {@link Student}를 반환하므로 프론트엔드(React)에서 바로 사용할 수 있습니다.
 */
@RestController
@RequestMapping("/api/students")
public class StudentRestController {

    private final StudentJpaRepository studentRepository;

    @Autowired
    public StudentRestController(StudentJpaRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * 모든 학생 목록을 조회합니다.
     *
     * @return 학생 리스트
     */
    @GetMapping("")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * 주어진 ID에 해당하는 학생을 조회합니다.
     *
     * @param id 학생 ID
     * @return 학생 정보 또는 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Integer id) {
        Optional<Student> studentOpt = studentRepository.findById(id);
        return studentOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * 새로운 학생을 생성합니다.
     *
     * @param student 생성할 학생 정보
     * @return 생성된 학생 정보
     */
    @PostMapping("")
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        Student saved = studentRepository.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 학생 정보를 업데이트합니다.
     *
     * @param id      업데이트할 학생의 ID
     * @param student 수정된 학생 정보
     * @return 업데이트된 학생 정보
     */
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Integer id, @RequestBody Student student) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        student.setId(id);
        Student updated = studentRepository.save(student);
        return ResponseEntity.ok(updated);
    }

    /**
     * 학생을 삭제합니다.
     *
     * @param id 삭제할 학생의 ID
     * @return 처리 결과
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Integer id) {
        if (!studentRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        studentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}