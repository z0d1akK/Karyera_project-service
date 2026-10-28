package com.zodiakk.projectservice.project.exception.project;

import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class InvalidProjectStatusTransitionException extends BusinessException {

    public InvalidProjectStatusTransitionException(ProjectStatus current, ProjectStatus target) {
        super(ErrorMessages.INVALID_PROJECT_STATUS_TRANSITION.formatted(current, target));
    }
}
