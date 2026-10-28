package com.zodiakk.projectservice.project.exception.projectrole;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class ProjectRoleInactiveException extends BusinessException {

    public ProjectRoleInactiveException(UUID id) {
        super(ErrorMessages.PROJECT_ROLE_INACTIVE.formatted(id));
    }
}
