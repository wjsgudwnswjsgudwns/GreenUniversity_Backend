package com.green.university.repository;

import com.green.university.dto.response.GradeDto;
import com.green.university.dto.response.GradeForScholarshipDto;
import com.green.university.dto.response.MyGradeDto;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Grade;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * JPA repository for {@link Grade} entities.
 */
public interface GradeJpaRepository extends JpaRepository<Grade, String> {

//    // 학생이 수강한 연도 조회, 연도 누계성적 조회
//    // Grade 리스트가져와서 -> 연도만 따로 다시 뽑아야함
//    public List<Grade> findByStudentIdAndSubYear(Integer studentId, Integer subYear);
//
//    // 학생이 수강한 학기 조회
//    // Grade 리스트가져와서 -> 학기만 따로 다시 뽑아야함
//    public List<Grade> findByStudentIdAndSemester(Integer studentId, Integer semester);
//
//    // 금학기 성적 조회
//    public List<Grade> findByStudentIdAndSemesterAndSubYear(Integer studentId, Integer semester, Integer subYear);
//
//    // 학기별 성적조회 (전체 조회)
//    public List<Grade> findByStudentId(Integer studentId);
//
//    // 학기별 성적조회 (선택 조회)
//    public List<Grade> findByStudentIdAndSubYearAndSemesterAndType(Integer studentId, Integer subYear, Integer semester, String type);
//
//    // 전체 찾는거, 전체 누계성적 조회, 장학금 유형 결정을 위한 성적 평균
//    public List<Grade> findByStudentIdAndSubYearAndSemester(Integer studentId, Integer subYear, Integer semester);
//
//    // 누계성적 조회(특정 학기까지의 성적을 합산/평균)
//    // 개별 성적 레코드들을 연도/학기 내림차순으로 정렬
//    public List<Grade> findByStudentIdOrderBySubYearDescSemesterDesc(Integer studentId);
//
////    // 장학금 유형 결정을 위한 성적 평균을 가져옴
////    List<Grade> findByStudentIdAndSubYearAndSemester(Integer studentId, Integer subYear, Integer semester);
//
////    // 전체 누계성적 조회
////     List<Grade> findByStudentIdAndSubYearAndSemester(Integer studentId, Integer subYear, Integer semester);
//
////    // 연도 누계성적 조회
////    public List<Grade> findByStudentIdAndSubYear(Integer studentId, Integer subYear);

}