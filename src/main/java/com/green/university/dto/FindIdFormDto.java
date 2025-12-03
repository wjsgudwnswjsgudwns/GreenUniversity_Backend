package com.green.university.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * id 찾기 폼
 * @author 김지현
 *
 */
@Data
public class FindIdFormDto {

	@NotBlank
	private String name;
	@Email
	private String email;
	private String userRole;
	
}
