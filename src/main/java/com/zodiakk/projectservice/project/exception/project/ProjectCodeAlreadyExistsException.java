package com.zodiakk.projectservice.project.exception.project;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class ProjectCodeAlreadyExistsException extends BusinessException {

    public ProjectCodeAlreadyExistsException(String code) {
        super(ErrorMessages.PROJECT_CODE_ALREADY_EXISTS.formatted(code));
    }
}
