package com.green.university.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.green.university.repository.EvaluationJpaRepository;
import com.green.university.repository.GradeJpaRepository;
import com.green.university.repository.StuSubJpaRepository;
import com.green.university.repository.model.Evaluation;
import com.green.university.repository.model.Grade;
import com.green.university.repository.model.StuSub;
import com.green.university.repository.model.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.response.GradeDto;
import com.green.university.dto.response.GradeForScholarshipDto;
import com.green.university.dto.response.MyGradeDto;

import com.green.university.utils.Define;

@Service
public class GradeService {

    @Autowired
    private GradeJpaRepository gradeJpaRepository;

    @Autowired
    private StuSubJpaRepository stuSubJpaRepository;

    @Autowired
    private EvaluationJpaRepository evaluationJpaRepository;

    // 학생이 수강한 연도 조회
    @Transactional(readOnly = true)
    public List<Integer> readGradeYearByStudentId(Integer studentId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentId(studentId);
        return stuSubs.stream()
                .map(ss -> ss.getSubject() != null ? ss.getSubject().getSubYear() : null)
                .filter(year -> year != null)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // 학생이 수강 신청한 학기 조회
    @Transactional(readOnly = true)
    public List<Integer> readGradeSemesterByStudentId(Integer studentId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentId(studentId);
        return stuSubs.stream()
                .map(ss -> ss.getSubject() != null ? ss.getSubject().getSemester() : null)
                .filter(semester -> semester != null)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    // 금학기 성적 조회
    @Transactional(readOnly = true)
    public List<GradeDto> readThisSemesterByStudentId(Integer studentId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId, Define.getCurrentYear(), Define.getCurrentSemester());

        return stuSubs.stream()
                .map(this::convertToGradeDto)
                .collect(Collectors.toList());
    }

    // 금학기 누계성적 조회
    @Transactional(readOnly = true)
    public MyGradeDto readMyGradeByStudentId(Integer studentId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId, Define.getCurrentYear(), Define.getCurrentSemester());

        double avgGrade = stuSubs.stream()
                .filter(ss -> ss.getGrade() != null)
                .mapToDouble(ss -> convertGradeToValue(ss.getGrade()))
                .average()
                .orElse(0.0);

        int sumGrades = stuSubs.stream()
                .filter(ss -> ss.getCompleteGrade() != null)
                .mapToInt(StuSub::getCompleteGrade)
                .sum();

        MyGradeDto myGradeDto = new MyGradeDto();
        myGradeDto.setStudentId(studentId);
        myGradeDto.setSubYear(Define.getCurrentYear());
        myGradeDto.setSemester(Define.getCurrentSemester());
        myGradeDto.setAverage((float) avgGrade);
        myGradeDto.setMyGrades(sumGrades);
        // sumGrades는 이수해야 할 학점이므로 Subject의 grades 합계 필요
        int totalGrades = stuSubs.stream()
                .filter(ss -> ss.getSubject() != null)
                .mapToInt(ss -> ss.getSubject().getGrades())
                .sum();
        myGradeDto.setSumGrades(totalGrades);

        return myGradeDto;
    }

    // 전체 누계성적 조회
    @Transactional(readOnly = true)
    public List<MyGradeDto> readgradeinquiryList(Integer studentId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentId(studentId);

        // 연도-학기별로 그룹핑
        Map<String, List<StuSub>> groupedStuSubs = stuSubs.stream()
                .filter(ss -> ss.getSubject() != null)
                .collect(Collectors.groupingBy(ss ->
                        ss.getSubject().getSubYear() + "-" + ss.getSubject().getSemester()));

        return groupedStuSubs.entrySet().stream()
                .map(entry -> {
                    List<StuSub> stuSubList = entry.getValue();

                    double avgGrade = stuSubList.stream()
                            .filter(ss -> ss.getGrade() != null)
                            .mapToDouble(ss -> convertGradeToValue(ss.getGrade()))
                            .average()
                            .orElse(0.0);

                    int myGrades = stuSubList.stream()
                            .filter(ss -> ss.getCompleteGrade() != null)
                            .mapToInt(StuSub::getCompleteGrade)
                            .sum();

                    int totalGrades = stuSubList.stream()
                            .filter(ss -> ss.getSubject() != null)
                            .mapToInt(ss -> ss.getSubject().getGrades())
                            .sum();

                    MyGradeDto dto = new MyGradeDto();
                    dto.setStudentId(studentId);
                    dto.setSubYear(stuSubList.get(0).getSubject().getSubYear());
                    dto.setSemester(stuSubList.get(0).getSubject().getSemester());
                    dto.setAverage((float) avgGrade);
                    dto.setMyGrades(myGrades);
                    dto.setSumGrades(totalGrades);

                    return dto;
                })
                .sorted(Comparator.comparing(MyGradeDto::getSubYear).thenComparing(MyGradeDto::getSemester))
                .collect(Collectors.toList());
    }

    // 학기별 성적조회 (전체 조회)
    @Transactional(readOnly = true)
    public List<GradeDto> readAllGradeByStudentId(Integer studentId) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentId(studentId);
        return stuSubs.stream()
                .map(this::convertToGradeDto)
                .collect(Collectors.toList());
    }

    // 학기별 성적조회 조회 (선택 조회)
    @Transactional(readOnly = true)
    public List<GradeDto> readGradeByType(Integer studentId, Integer subYear, Integer semester, String type) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_SemesterAndSubject_Type(
                studentId, subYear, semester, type);
        return stuSubs.stream()
                .map(this::convertToGradeDto)
                .collect(Collectors.toList());
    }

    // 전체일때 조회
    @Transactional(readOnly = true)
    public List<GradeDto> readGradeByStudentId(Integer studentId, Integer subYear, Integer semester) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId, subYear, semester);
        return stuSubs.stream()
                .map(this::convertToGradeDto)
                .collect(Collectors.toList());
    }

    // 성적 평균 가져오기
    @Transactional(readOnly = true)
    public GradeForScholarshipDto readAvgGrade(Integer studentId, Integer subYear, Integer semester) {
        List<StuSub> stuSubs = stuSubJpaRepository.findByStudentIdAndSubject_SubYearAndSubject_Semester(
                studentId, subYear, semester);


        double avgGrade = stuSubs.stream()
                .filter(ss -> ss.getGrade() != null)
                .mapToDouble(ss -> convertGradeToValue(ss.getGrade()))
                .average()
                .orElse(0.0);

        GradeForScholarshipDto dto = new GradeForScholarshipDto();
        dto.setAvgGrade(avgGrade);

        return dto;
    }

    // StuSub을 GradeDto로 변환
    private GradeDto convertToGradeDto(StuSub stuSub) {
        GradeDto dto = new GradeDto();
        Subject subject = stuSub.getSubject();

        dto.setSubYear(subject != null ? subject.getSubYear() : null);
        dto.setSemester(subject != null ? subject.getSemester() : null);
        dto.setSubjectId(stuSub.getSubjectId());
        dto.setName(subject != null ? subject.getName() : null);
        dto.setType(subject != null ? subject.getType() : null);
        dto.setGrade(stuSub.getGrade());

        // 이수학점 설정 - Subject의 grades 사용 (수정!)
        dto.setGrades(subject != null ? String.valueOf(subject.getGrades()) : null);

        dto.setGradeValue(stuSub.getGrade() != null ? String.valueOf(convertGradeToValue(stuSub.getGrade())) : null);

        // 강의평가 여부 확인
        Evaluation evaluation = evaluationJpaRepository
                .findByStudentIdAndSubjectId(stuSub.getStudentId(), stuSub.getSubjectId())
                .orElse(null);
        dto.setEvaluationId(evaluation != null ? evaluation.getEvaluationId() : null);

        return dto;
    }

    // 학점을 숫자로 변환 (A+ → 4.5 등)
    private double convertGradeToValue(String gradeStr) {
        Grade grade = gradeJpaRepository.findById(gradeStr).orElse(null);
        return grade != null ? grade.getGradeValue() : 0.0;
    }

}
