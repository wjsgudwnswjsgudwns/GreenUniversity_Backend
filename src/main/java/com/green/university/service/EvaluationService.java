package com.green.university.service;

import java.util.List;
import java.util.stream.Collectors;

import com.green.university.repository.EvaluationJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.EvaluationDto;
import com.green.university.dto.MyEvaluationDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Evaluation;

@Service
public class EvaluationService {

    @Autowired
    private EvaluationJpaRepository evaluationJpaRepository;

    @Transactional
    public void createEvanluation(EvaluationDto evaluationFormDto) {

        // DTO를 Entity로 변환
        Evaluation evaluation = new Evaluation();
        evaluation.setStudentId(evaluationFormDto.getStudentId());
        evaluation.setSubjectId(evaluationFormDto.getSubjectId());
        evaluation.setAnswer1(evaluationFormDto.getAnswer1());
        evaluation.setAnswer2(evaluationFormDto.getAnswer2());
        evaluation.setAnswer3(evaluationFormDto.getAnswer3());
        evaluation.setAnswer4(evaluationFormDto.getAnswer4());
        evaluation.setAnswer5(evaluationFormDto.getAnswer5());
        evaluation.setAnswer6(evaluationFormDto.getAnswer6());
        evaluation.setAnswer7(evaluationFormDto.getAnswer7());
        evaluation.setImprovements(evaluationFormDto.getImprovements());

        // JPA save
        Evaluation savedEvaluation = evaluationJpaRepository.save(evaluation);

        if (savedEvaluation == null || savedEvaluation.getEvaluationId() == null) {
            throw new CustomRestfullException("강의평가 등록이 실패하였습니다", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	// 강의평가 조회 (학생)
    @Transactional(readOnly = true)
    public Evaluation readEvaluationByStudentIdAndSubjectId(Integer studentId, Integer subjectId) {
        Evaluation evaluation = evaluationJpaRepository.findByStudentIdAndSubjectId(studentId, subjectId).orElse(null);
        return evaluation;
    }

    // 전체 강의평가 조회 (교수)
    @Transactional(readOnly = true)
    public List<MyEvaluationDto> readEvaluationByProfessorId(Integer professorId) {
        List<Evaluation> evaluations = evaluationJpaRepository.findBySubject_ProfessorId(professorId);

        // Entity를 DTO로 변환
        List<MyEvaluationDto> result = evaluations.stream()
                .map(e -> {
                    MyEvaluationDto dto = new MyEvaluationDto();
                    dto.setProfessorId(professorId);
                    dto.setName(e.getStudent() != null ? e.getStudent().getName() : null);
                    dto.setAnswer1(e.getAnswer1());
                    dto.setAnswer2(e.getAnswer2());
                    dto.setAnswer3(e.getAnswer3());
                    dto.setAnswer4(e.getAnswer4());
                    dto.setAnswer5(e.getAnswer5());
                    dto.setAnswer6(e.getAnswer6());
                    dto.setAnswer7(e.getAnswer7());
                    dto.setImprovements(e.getImprovements());
                    return dto;
                })
                .collect(Collectors.toList());

        return result;
    }

    // 과목별 강의평가 조회 (교수)
    @Transactional(readOnly = true)
    public List<MyEvaluationDto> readEvaluationByProfessorIdAndName(Integer professorId, String name) {
        List<Evaluation> evaluations = evaluationJpaRepository.findBySubject_ProfessorIdAndSubject_Name(professorId, name);

        // Entity를 DTO로 변환
        List<MyEvaluationDto> result = evaluations.stream()
                .map(e -> {
                    MyEvaluationDto dto = new MyEvaluationDto();
                    dto.setProfessorId(professorId);
                    dto.setName(e.getStudent() != null ? e.getStudent().getName() : null);
                    dto.setAnswer1(e.getAnswer1());
                    dto.setAnswer2(e.getAnswer2());
                    dto.setAnswer3(e.getAnswer3());
                    dto.setAnswer4(e.getAnswer4());
                    dto.setAnswer5(e.getAnswer5());
                    dto.setAnswer6(e.getAnswer6());
                    dto.setAnswer7(e.getAnswer7());
                    dto.setImprovements(e.getImprovements());
                    return dto;
                })
                .collect(Collectors.toList());

        return result;
    }

    @Transactional(readOnly = true)
    public List<String> readSubjectName(Integer professorId) {
        List<Evaluation> evaluations = evaluationJpaRepository.findBySubject_ProfessorId(professorId);

        // 과목명만 추출 (중복 제거)
        List<String> subjectNames = evaluations.stream()
                .map(e -> e.getSubject().getName())
                .distinct()
                .collect(Collectors.toList());

        return subjectNames;
    }
}
