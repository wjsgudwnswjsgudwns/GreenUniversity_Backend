package com.green.university.dto;

import jakarta.validation.
constraints.*;

import lombok.Data;

@Data
public class LoginDto {

    @NotNull(message = "아이디는 필수입니다.")
    @Min(value = 100000, message = "아이디는 6자리 이상 숫자여야 합니다.")
    @Max(value = 2147483646, message = "아이디 값이 너무 큽니다.")
	private Integer id;
    @NotBlank(message = "패스워드는 필수입니다.")
    @Size(min = 6, max = 20, message = "패스워드는 6~20자 사이여야합니다.")
	private String password;
	private String rememberId;
	
}
