package com.zodiakk.projectservice.project.exception.assignment;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class ProjectAssignmentAlreadyExistsException extends BusinessException {

    public ProjectAssignmentAlreadyExistsException(UUID employeeId, UUID projectId) {
        super(ErrorMessages.PROJECT_ASSIGNMENT_ALREADY_EXISTS.formatted(employeeId, projectId));
    }
}
