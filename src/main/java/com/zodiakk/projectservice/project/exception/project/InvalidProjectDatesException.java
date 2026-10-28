package com.zodiakk.projectservice.project.exception.project;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class InvalidProjectDatesException extends BusinessException {

    public InvalidProjectDatesException() {
        super(ErrorMessages.INVALID_PROJECT_DATES);
    }
}
