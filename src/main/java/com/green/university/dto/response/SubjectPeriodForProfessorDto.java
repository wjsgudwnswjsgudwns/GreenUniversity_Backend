package com.green.university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 교수의 과목 학기별 조회를 위한
 * 학기 select dto
 * @author 김지현
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPeriodForProfessorDto {
	
	private Integer id;
	private Integer subYear;
	private Integer semester;

}
