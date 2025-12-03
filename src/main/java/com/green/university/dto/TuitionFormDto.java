package com.green.university.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 
 * @author 박성희
 *
 */
@Data
public class TuitionFormDto {
	@NotEmpty
	@Size(min = 10000000)
	@Size(max = 99999999)
	private String studentId;
	@NotEmpty
	@Size(max = 6)
	private String semester;
	private boolean status;
}
