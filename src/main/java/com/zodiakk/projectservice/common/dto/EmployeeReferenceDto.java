package com.zodiakk.projectservice.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeReferenceDto {

    @Schema(description = "Employee identifier")
    private UUID employeeId;

    @Schema(description = "Employee full name")
    private String fullName;

    @Schema(description = "Whether the employee is active")
    private Boolean active;
}
