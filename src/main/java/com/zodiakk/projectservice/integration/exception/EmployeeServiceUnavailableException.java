package com.zodiakk.projectservice.integration.exception;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class EmployeeServiceUnavailableException extends BusinessException {

    public EmployeeServiceUnavailableException(Throwable cause) {
        super(ErrorMessages.EMPLOYEE_SERVICE_UNAVAILABLE);
        initCause(cause);
    }
}
