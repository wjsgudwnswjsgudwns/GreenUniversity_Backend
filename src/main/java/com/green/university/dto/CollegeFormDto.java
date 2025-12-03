package com.green.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;
/**
 * 
 * @author 박성희
 *
 */
@Data
public class CollegeFormDto {
	@NotBlank
	private String name;
}
