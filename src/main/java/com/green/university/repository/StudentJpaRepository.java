package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Student;

/**
 * Spring Data JPA repository for {@link Student} entities.
 *
 * 이 인터페이스는 MyBatis를 사용하지 않고 학생 데이터를 조회하거나 저장하기 위해 사용됩니다.
 */
public interface StudentJpaRepository extends JpaRepository<Student, Integer> {
    // Additional query methods can be defined here if necessary
}