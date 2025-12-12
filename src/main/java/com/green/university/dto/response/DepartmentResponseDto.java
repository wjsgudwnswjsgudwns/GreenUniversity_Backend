package com.green.university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseDto {
    private Integer id;
    private String name;
    private Integer collegeId;
    private String collegeName;

    // Department 엔티티에서 DTO로 변환하는 정적 메서드
    public static DepartmentResponseDto fromEntity(com.green.university.repository.model.Department department) {
        return new DepartmentResponseDto(
                department.getId(),
                department.getName(),
                department.getCollege() != null ? department.getCollege().getId() : null,
                department.getCollege() != null ? department.getCollege().getName() : null
        );
    }
}