package com.green.university.service;

import com.green.university.repository.AICounselingRepository;
import com.green.university.repository.model.AICounseling;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AICounselingQueryService {

    private final AICounselingRepository aiCounselingRepository;

    public List<AICounseling> getCompletedCounselingsForAnalysis(Integer studentId) {
        return aiCounselingRepository
                .findCompletedCounselingsWithContentByStudentId(studentId);
    }

    /**
     * 학생의 특정 과목에 대한 완료된 상담 조회 (과목별)
     */
    public List<AICounseling> getCompletedCounselingsForAnalysisBySubject(
            Integer studentId, Integer subjectId) {
        return aiCounselingRepository
                .findCompletedCounselingsWithContentByStudentIdAndSubjectId(studentId, subjectId);
    }
}