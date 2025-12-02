package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
public class CollegeService {

    @Autowired
    private CollegeJpaRepository collegeJpaRepository;

    @Autowired
    private DepartmentJpaRepository departmentJpaRepository;

	//  id로 해당 단과대 정보 가져옴
    public College readCollById(Integer id) {
        return collegeJpaRepository.findById(id).orElse(null);
    }

	// id로 해당 학과 정보 가져옴
    public Department readDeptById(Integer id) {
        return departmentJpaRepository.findById(id).orElse(null);
    }

	// 전체 학과 정보 조회
    public List<Department> readDeptAll() {
        return departmentJpaRepository.findAll();
    }

}
