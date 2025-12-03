package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.ScheduleDto;
import com.green.university.dto.ScheduleFormDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.ScheduleJpaRepository;
import com.green.university.repository.StaffJpaRepository;
import com.green.university.repository.model.Schedule;
import com.green.university.repository.model.Staff;

@Service
public class ScheuleService { // todo ScheduleService로 변경

    // JPA Repositories for Schedule and Staff
    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private StaffJpaRepository staffJpaRepository;

    // 학사일정 조회
    public List<Schedule> readSchedule() {
        // JPA를 사용하여 전체 학사일정 목록을 반환한다.
        return scheduleJpaRepository.findAll();
    }

    // 학사일정 조회 (디테일)
    /**
     * 지정된 ID의 학사일정을 조회하여 ScheduleDto 형태로 반환한다.
     *
     * 기존에는 MyBatis를 통해 ScheduleDto를 직접 조회했으나, JPA로 전환하며
     * Schedule 엔티티를 조회하여 필요한 필드를 DTO에 매핑한다. 만약 찾을 수 없는 경우
     * null을 반환한다.
     *
     * @param id 학사일정 ID
     * @return 학사일정 DTO 또는 null
     */
    public ScheduleDto readScheduleById(Integer id) {
        if (id == null) {
            return null;
        }
        Schedule schedule = scheduleJpaRepository.findById(id).orElse(null);
        if (schedule == null) {
            return null;
        }
        ScheduleDto dto = new ScheduleDto();
        // 학사 일정 기본 정보 매핑
        dto.setId(schedule.getId());
        // Staff ID 매핑
        Staff staff = schedule.getStaff();
        if (staff != null) {
            dto.setStaffId(staff.getId());
        }
        // 날짜 정보는 문자열 형태로 저장한다 (yyyy-MM-dd)
        if (schedule.getStartDay() != null) {
            java.time.LocalDate localStart = schedule.getStartDay().toLocalDate();
            dto.setStartDay(localStart.toString());
            dto.setStartMday(localStart.getDayOfMonth() + "");
            dto.setYears(localStart.getYear());
            dto.setMouths(localStart.getMonthValue());
        }
        if (schedule.getEndDay() != null) {
            java.time.LocalDate localEnd = schedule.getEndDay().toLocalDate();
            dto.setEndDay(localEnd.toString());
            dto.setEndMday(localEnd.getDayOfMonth() + "");
        }
        dto.setInformation(schedule.getInformation());
        // sum 필드는 MyBatis 버전에서 월별 집계를 위한 값이므로 기본 null로 둔다
        dto.setSum(null);
        return dto;
    }

    // 학사일정 추가
    @Transactional
    public void createSchedule(Integer staffId, ScheduleFormDto dto) {
        // JPA를 사용하여 학사일정을 생성한다. staffId로 Staff 엔티티를 조회하여 연관관계를 설정한다.
        Staff staff = staffJpaRepository.findById(staffId)
                .orElseThrow(() -> new CustomRestfullException("등록한 교직원 정보가 존재하지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
        Schedule schedule = new Schedule();
        schedule.setStaff(staff);
        // Schedule 엔티티의 startDay, endDay는 java.sql.Date 타입이다. DTO는 문자열이므로 변환한다.
        try {
            schedule.setStartDay(java.sql.Date.valueOf(dto.getStartDay()));
            schedule.setEndDay(java.sql.Date.valueOf(dto.getEndDay()));
        } catch (Exception e) {
            throw new CustomRestfullException("날짜 형식이 올바르지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        schedule.setInformation(dto.getInformation());
        scheduleJpaRepository.save(schedule);
    }

    // 학사일정 업데이트
    @Transactional
    public int updateSchedule(ScheduleFormDto scheduleFormDto) {
        if (scheduleFormDto.getId() == null) {
            return 0;
        }
        return scheduleJpaRepository.findById(scheduleFormDto.getId()).map(entity -> {
            // 날짜는 문자열로 전달되므로 java.sql.Date로 변환한다.
            try {
                if (scheduleFormDto.getStartDay() != null) {
                    entity.setStartDay(java.sql.Date.valueOf(scheduleFormDto.getStartDay()));
                }
                if (scheduleFormDto.getEndDay() != null) {
                    entity.setEndDay(java.sql.Date.valueOf(scheduleFormDto.getEndDay()));
                }
            } catch (Exception e) {
                throw new CustomRestfullException("날짜 형식이 올바르지 않습니다", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            entity.setInformation(scheduleFormDto.getInformation());
            scheduleJpaRepository.save(entity);
            return 1;
        }).orElse(0);
    }

    // 학사일정 삭제
    @Transactional
    public int deleteSchedule(Integer id) {
        if (id == null) {
            return 0;
        }
        scheduleJpaRepository.deleteById(id);
        return 1;
    }

    // 학사일정 월에 있는 일정 조회
    //
    // 기존 MyBatis 구현은 scheuleRepository.selectSchoduleMouth()를 통해
    // 월별 학사일정 집계 결과를 반환했다. JPA로 전환하면서 단순히 모든 학사일정
    // 엔티티를 조회한 뒤 DTO로 변환하는 방식으로 구현한다. sum 필드는 집계용으로
    // 사용되던 값이나 이 구현에서는 별도로 계산하지 않으므로 null로 둔다.
    @Transactional(readOnly = true)
    public List<ScheduleDto> readScheduleDto() {
        // 모든 일정 조회
        List<Schedule> schedules = scheduleJpaRepository.findAll();
        List<ScheduleDto> result = new java.util.ArrayList<>();
        for (Schedule s : schedules) {
            ScheduleDto dto = new ScheduleDto();
            // 기본 정보 매핑
            dto.setId(s.getId());
            if (s.getStaff() != null) {
                dto.setStaffId(s.getStaff().getId());
            }
            // 시작 날짜
            if (s.getStartDay() != null) {
                java.time.LocalDate start = s.getStartDay().toLocalDate();
                dto.setStartDay(start.toString());
                dto.setStartMday(Integer.toString(start.getDayOfMonth()));
                dto.setYears(start.getYear());
                dto.setMouths(start.getMonthValue());
            }
            // 종료 날짜
            if (s.getEndDay() != null) {
                java.time.LocalDate end = s.getEndDay().toLocalDate();
                dto.setEndDay(end.toString());
                dto.setEndMday(Integer.toString(end.getDayOfMonth()));
            }
            dto.setInformation(s.getInformation());
            // sum은 월별 집계 값이 없으므로 null 설정
            dto.setSum(null);
            result.add(dto);
        }
        return result;
    }
	
    // 월별 학사일정 조회
    @Transactional
    public List<Schedule> readScheduleListByMonth(Integer month) {
        if (month == null) {
            return java.util.Collections.emptyList();
        }
        List<Schedule> all = scheduleJpaRepository.findAll();
        List<Schedule> result = new java.util.ArrayList<>();
        for (Schedule s : all) {
            if (s.getStartDay() != null) {
                int m = s.getStartDay().toLocalDate().getMonthValue();
                if (m == month) {
                    result.add(s);
                }
            }
        }
        return result;
    }
}
