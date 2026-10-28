package com.zodiakk.projectservice.integration.exception;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class EmployeeReferenceInactiveException extends BusinessException {

    public EmployeeReferenceInactiveException(UUID employeeId) {
        super(ErrorMessages.EMPLOYEE_REFERENCE_INACTIVE.formatted(employeeId));
    }
}
