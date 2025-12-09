package com.green.university.repository;

import com.green.university.repository.model.College;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Subject;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Subject} entities.
 *
 * 기존 MyBatis 매퍼를 대체하기 위한 인터페이스입니다. 필요한 경우
 * JPA 쿼리 메서드를 추가 정의하여 사용할 수 있습니다.
 */
public interface SubjectJpaRepository extends JpaRepository<Subject, Integer> {
    // 추가적인 조회 메서드는 필요 시 선언

    List<Subject> findByRoom_IdAndSubDayAndSubYearAndSemester(String roomId, String subDay, Integer subYear, Integer semester);

    List<Subject> findByProfessor_Id(Integer professorId);

    List<Subject> findByProfessor_IdAndSubYearAndSemester(Integer professorId, Integer subYear, Integer semester);

    @Query("SELECT s FROM Subject s WHERE s.capacity >= s.numOfStudent")
    List<Subject> findByCapacityGreaterThanEqualNumOfStudent();

    @Query("SELECT s FROM Subject s WHERE s.capacity < s.numOfStudent")
    List<Subject> findByCapacityLessThanNumOfStudent();

    List<Subject> findAllByOrderByIdAsc();

    // 페이징
    Page<Subject> findAllByOrderByIdAsc(Pageable pageable);

    // 이름으로 검색 (LIKE 검색, 페이징 지원)
    Page<Subject> findByNameContainingOrderByIdAsc(String name, Pageable pageable);

    // 이름으로 검색 (LIKE 검색, 페이징 없음 - 수정용)
    List<Subject> findByNameContainingOrderByIdAsc(String name);

    /**
     * 정원 >= 예비 신청 인원인 과목 조회
     */
    @Query("SELECT s FROM Subject s WHERE s.capacity >= s.preNumOfStudent " +
            "AND s.subYear = :year AND s.semester = :semester")
    List<Subject> findByCapacityGreaterThanEqualPreNumOfStudent(
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

    /**
     * 정원 < 예비 신청 인원인 과목 조회
     */
    @Query("SELECT s FROM Subject s WHERE s.capacity < s.preNumOfStudent " +
            "AND s.subYear = :year AND s.semester = :semester")
    List<Subject> findByCapacityLessThanPreNumOfStudent(
            @Param("year") Integer year,
            @Param("semester") Integer semester
    );

}
