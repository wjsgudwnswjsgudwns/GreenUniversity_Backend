package com.green.university.repository;

import com.green.university.repository.model.Evaluation;
import com.green.university.repository.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Grade} entities.
 */
public interface EvaluationJpaRepository extends JpaRepository<Evaluation, Integer> {

    Optional<Evaluation> findByStudentIdAndSubjectId(Integer studentId, Integer subjectId);

    List<Evaluation> findBySubject_ProfessorId(Integer professorId);

    List<Evaluation> findBySubject_ProfessorIdAndSubject_Name(Integer professorId, String name);
}