package com.green.university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Student;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Student} entities.
 *
 * 이 인터페이스는 MyBatis를 사용하지 않고 학생 데이터를 조회하거나 저장하기 위해 사용됩니다.
 */
public interface StudentJpaRepository extends JpaRepository<Student, Integer> {
    // Additional query methods can be defined here if necessary
    Optional<Student> findByIdAndNameAndEmail(Integer id, String name, String email);
    Optional<Student> findByNameAndEmail(String name, String email);

    // 학과별 학생 조회 (페이징)
    Page<Student> findByDepartmentId(Integer deptId, Pageable pageable);

    // 학과별 학생 수
    long countByDepartmentId(Integer deptId);

    // 학번으로 조회 (페이징)
    @Query("SELECT s FROM Student s WHERE s.id = :studentId")
    Page<Student> findById(@Param("studentId") Integer studentId, Pageable pageable);

    /**
     * 특정 교수를 담당 교수로 하는 학생 수를 조회합니다.
     */
    long countByAdvisorId(Integer advisorId);

    /**
     * 담당 교수가 없는 모든 학생을 조회합니다.
     */
    List<Student> findByAdvisorIsNull();

    /**
     * 특정 학과에 소속되어 있고 담당 교수가 없는 학생들을 조회합니다.
     */
    List<Student> findByDeptIdAndAdvisorIsNull(Integer deptId);

    /**
     * 특정 학과에 소속되어 있고 담당 교수가 있는 학생들을 조회합니다.
     */
    List<Student> findByDeptIdAndAdvisorIsNotNull(Integer deptId);

    /**
     * 특정 학과의 모든 학생을 조회합니다.
     */
    List<Student> findByDeptId(Integer deptId);

    /**
     * 특정 교수를 담당 교수로 하는 학생 목록을 조회합니다.
     */
    List<Student> findByAdvisorId(Integer advisorId);

    List<Student> findByGrade(Integer grade);
}