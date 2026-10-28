package com.zodiakk.projectservice.integration.exception;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class EmployeeReferenceNotFoundException extends ResourceNotFoundException {

    public EmployeeReferenceNotFoundException(UUID employeeId) {
        super(ErrorMessages.EMPLOYEE_REFERENCE_NOT_FOUND.formatted(employeeId));
    }
}
