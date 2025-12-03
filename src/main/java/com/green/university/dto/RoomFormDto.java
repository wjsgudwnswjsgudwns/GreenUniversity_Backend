package com.green.university.dto;

import jakarta.validation.
constraints.NotNull;
import jakarta.validation.
constraints.Size;

import lombok.Data;
/**
 * 
 * @author 박성희
 *
 */
@Data
public class RoomFormDto {
	@NotNull
	@Size(min = 4, max = 4)
	private String id;
	@NotNull
	private String collegeId;
}
