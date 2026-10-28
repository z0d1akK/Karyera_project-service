package com.zodiakk.projectservice.integration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmployeeLookupResponse(
        List<EmployeeLookupItem> employees
) {
}
