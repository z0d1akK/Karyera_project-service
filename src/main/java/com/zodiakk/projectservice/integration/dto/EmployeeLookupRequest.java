package com.zodiakk.projectservice.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmployeeLookupRequest(
        List<UUID> employeeIds
) {
}
