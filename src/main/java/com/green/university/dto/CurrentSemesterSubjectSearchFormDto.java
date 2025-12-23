package com.green.university.dto;

import jakarta.validation.constraints.NotBlank;

import com.green.university.utils.Define;

import lombok.Data;

@Data
public class CurrentSemesterSubjectSearchFormDto {

	private String type;
	private Integer deptId;
	private String name;
	
	private Integer subYear = Define.getCurrentYear();
	private Integer semester = Define.getCurrentSemester();
	
	private Integer page;
	
}
