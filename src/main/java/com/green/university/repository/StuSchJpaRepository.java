package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.StuSch;
import com.green.university.repository.model.StuSchId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * JPA repository for {@link StuSch} entities.
 */
public interface StuSchJpaRepository extends JpaRepository<StuSch, StuSchId> {
    // stu_sch_tb + scholarship_tb JOIN 해서 장학 정보까지 한 번에 가져오기
    @Query("SELECT ss FROM StuSch ss " +
            "JOIN FETCH ss.scholarship s " +
            "WHERE ss.id.studentId = :studentId " +
            "AND ss.id.schYear = :year " +
            "AND ss.id.semester = :semester")
    Optional<StuSch> findWithScholarship(@Param("studentId") Integer studentId,
                                         @Param("year") Integer year,
                                         @Param("semester") Integer semester);
}