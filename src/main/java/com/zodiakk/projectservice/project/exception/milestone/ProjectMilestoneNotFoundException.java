package com.zodiakk.projectservice.project.exception.milestone;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectMilestoneNotFoundException extends ResourceNotFoundException {

    public ProjectMilestoneNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_MILESTONE_NOT_FOUND.formatted(id));
    }
}
