package com.green.university.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.green.university.repository.CollegeJpaRepository;
import com.green.university.repository.DepartmentJpaRepository;
import com.green.university.repository.model.College;
import com.green.university.repository.model.Department;

/**
 *
 * @author 서영
 *
 */
@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeJpaRepository collegeJpaRepository;
    private final DepartmentJpaRepository departmentJpaRepository;

    /**
     * id로 해당 단과대 정보 가져옴
     */
    public College readCollById(Integer id) {
        return collegeJpaRepository.findById(id).orElse(null);
    }

    /**
     * id로 해당 학과 정보 가져옴
     */
    public Department readDeptById(Integer id) {
        return departmentJpaRepository.findById(id).orElse(null);
    }

    /**
     * 전체 학과 정보 조회
     */
    public List<Department> readDeptAll() {
        return departmentJpaRepository.findAll();
    }

    /**
     * 학과 ID로 학과명과 단과대명을 함께 조회
     * @param deptId 학과 ID
     * @return Map {"deptName": "학과명", "collName": "단과대명"}
     */
    public Map<String, String> readDeptAndCollNameByDeptId(Integer deptId) {
        Department dept = readDeptById(deptId);
        if (dept == null) {
            return null;
        }

        // Department 엔티티는 College 객체를 직접 가지고 있음
        College college = dept.getCollege();
        if (college == null) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        result.put("deptName", dept.getName());
        result.put("collName", college.getName());
        return result;
    }

    /**
     * 학과 ID로 단과대 정보 조회
     * @param deptId 학과 ID
     * @return College
     */
    public College readCollegeByDeptId(Integer deptId) {
        Department dept = readDeptById(deptId);
        if (dept == null) {
            return null;
        }
        // Department 엔티티는 College 객체를 직접 가지고 있음
        return dept.getCollege();
    }

}