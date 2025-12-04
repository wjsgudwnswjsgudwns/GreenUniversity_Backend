package com.green.university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyllabusResponseDto {
    private Integer subjectId;
    private String subjectName;
    private String professorName;

    private String classTime;
    private String roomId;

    private Integer subYear;
    private Integer semester;
    private Integer grades;
    private String type;

    private String deptName;
    private String tel;
    private String email;

    private String overview;
    private String objective;
    private String textbook;
    private String program;
}
