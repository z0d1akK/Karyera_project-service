package com.zodiakk.projectservice.project.exception.projectrole;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class ProjectRoleCodeAlreadyExistsException extends BusinessException {

    public ProjectRoleCodeAlreadyExistsException(String code) {
        super(ErrorMessages.PROJECT_ROLE_CODE_ALREADY_EXISTS.formatted(code));
    }
}
