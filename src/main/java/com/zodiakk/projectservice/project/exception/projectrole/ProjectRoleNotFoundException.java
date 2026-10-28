package com.zodiakk.projectservice.project.exception.projectrole;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectRoleNotFoundException extends ResourceNotFoundException {

    public ProjectRoleNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_ROLE_NOT_FOUND.formatted(id));
    }
}
