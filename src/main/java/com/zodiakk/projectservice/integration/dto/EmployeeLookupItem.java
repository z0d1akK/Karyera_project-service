package com.zodiakk.projectservice.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmployeeLookupItem(
        UUID id,
        String fullName,
        boolean active,
        UUID positionId,
        UUID gradeId
) {
}
