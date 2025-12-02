package com.green.university.repository;

import com.green.university.repository.model.StuSubDetail;
import com.green.university.repository.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StuSubDetailJpaRepository extends JpaRepository<StuSubDetail,Integer>{

    Optional<StuSubDetail> findByStudentIdAndSubjectId(Integer studentId, Integer subjectId);

}
