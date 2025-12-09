package com.green.university.repository;

import com.green.university.repository.model.SugangPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SugangPeriodJpaRepository extends JpaRepository<SugangPeriod, Integer> {

    /**
     * 특정 학년도, 학기의 수강 신청 기간 조회
     */
    Optional<SugangPeriod> findByYearAndSemester(Integer year, Integer semester);

    /**
     * 최신 수강 신청 기간 조회
     */
    @Query("SELECT sp FROM SugangPeriod sp ORDER BY sp.year DESC, sp.semester DESC LIMIT 1")
    Optional<SugangPeriod> findLatest();
}