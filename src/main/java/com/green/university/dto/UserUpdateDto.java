package com.green.university.dto;

import jakarta.validation.
constraints.Email;
import jakarta.validation.
constraints.NotBlank;
import jakarta.validation.
constraints.NotEmpty;

import lombok.Data;

@Data
public class UserUpdateDto {
	
	private Integer userId;
	@NotEmpty
	private String address;
	@NotBlank
	private String tel;
	@Email
	private String email;
	
}
