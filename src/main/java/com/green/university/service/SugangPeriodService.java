package com.green.university.service;

import com.green.university.repository.SugangPeriodJpaRepository;
import com.green.university.repository.model.SugangPeriod;
import com.green.university.utils.Define;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SugangPeriodService {

    @Autowired
    private SugangPeriodJpaRepository sugangPeriodJpaRepository;

    /**
     * 현재 학기의 수강 신청 기간 조회
     */
    @Transactional(readOnly = true)
    public SugangPeriod getCurrentPeriod() {
        return sugangPeriodJpaRepository
                .findByYearAndSemester(Define.CURRENT_YEAR, Define.CURRENT_SEMESTER)
                .orElseGet(() -> {
                    // 없으면 생성
                    SugangPeriod newPeriod = new SugangPeriod();
                    newPeriod.setPeriod(0);
                    newPeriod.setYear(Define.CURRENT_YEAR);
                    newPeriod.setSemester(Define.CURRENT_SEMESTER);
                    return sugangPeriodJpaRepository.save(newPeriod);
                });
    }

    /**
     * 수강 신청 기간 업데이트
     */
    @Transactional
    public SugangPeriod updatePeriod(Integer newPeriod) {
        SugangPeriod sugangPeriod = getCurrentPeriod();
        sugangPeriod.setPeriod(newPeriod);
        return sugangPeriodJpaRepository.save(sugangPeriod);
    }

    /**
     * 현재 기간 값 가져오기
     */
    @Transactional(readOnly = true)
    public Integer getPeriodValue() {
        return getCurrentPeriod().getPeriod();
    }
}