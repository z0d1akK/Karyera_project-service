package com.zodiakk.projectservice.project.exception.assignment;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class InvalidAssignmentDatesException extends BusinessException {

    public InvalidAssignmentDatesException() {
        super(ErrorMessages.INVALID_ASSIGNMENT_DATES);
    }
}
