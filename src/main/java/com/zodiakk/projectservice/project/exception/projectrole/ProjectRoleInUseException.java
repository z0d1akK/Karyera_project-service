package com.zodiakk.projectservice.project.exception.projectrole;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class ProjectRoleInUseException extends BusinessException {

    public ProjectRoleInUseException(UUID id) {
        super(ErrorMessages.PROJECT_ROLE_IN_USE.formatted(id));
    }
}
