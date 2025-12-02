package com.green.university.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.response.QuestionDto;
import com.green.university.repository.QuestionJpaRepository;
import com.green.university.repository.model.Question;

/**
 * 서비스 레이어에서 설문 질문을 조회한다.
 *
 * 기존 MyBatis 기반 QuestionRepository를 제거하고 JPA 기반 QuestionJpaRepository를 사용한다.
 */
@Service
public class QuestionService {

    @Autowired
    private QuestionJpaRepository questionJpaRepository;

    /**
     * 설문 질문 전체를 조회하여 DTO 형태로 반환한다.
     *
     * question_tb 테이블에는 일반적으로 한 행만 존재하지만, 여러 행이 있을 경우
     * 첫 번째 행을 사용한다. 만약 데이터가 존재하지 않으면 null을 반환한다.
     *
     * @return 질문 DTO
     */
    @Transactional(readOnly = true)
    public QuestionDto readQuestion() {
        List<Question> list = questionJpaRepository.findAll();
        if (list == null || list.isEmpty()) {
            return null;
        }
        Question entity = list.get(0);
        QuestionDto dto = new QuestionDto();
        dto.setQuestion1(entity.getQuestion1());
        dto.setQuestion2(entity.getQuestion2());
        dto.setQuestion3(entity.getQuestion3());
        dto.setQuestion4(entity.getQuestion4());
        dto.setQuestion5(entity.getQuestion5());
        dto.setQuestion6(entity.getQuestion6());
        dto.setQuestion7(entity.getQuestion7());
        dto.setSugContent(entity.getSugContent());
        return dto;
    }
}
