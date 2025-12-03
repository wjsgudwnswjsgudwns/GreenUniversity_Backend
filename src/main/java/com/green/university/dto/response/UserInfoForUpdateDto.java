package com.green.university.dto.response;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class UserInfoForUpdateDto {

	@NotBlank
	private String address;
	@Size(min = 11, max = 13)
	private String tel;
	@Email
	private String email;
	
}
