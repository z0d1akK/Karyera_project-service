package com.zodiakk.projectservice.project.exception.project;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_NOT_FOUND.formatted(id));
    }
}
