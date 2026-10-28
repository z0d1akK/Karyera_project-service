package com.zodiakk.projectservice.project.exception.assignment;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectAssignmentNotFoundException extends ResourceNotFoundException {

    public ProjectAssignmentNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_ASSIGNMENT_NOT_FOUND.formatted(id));
    }
}
