package com.zodiakk.projectservice.project.exception.link;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectLinkNotFoundException extends ResourceNotFoundException {

    public ProjectLinkNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_LINK_NOT_FOUND.formatted(id));
    }
}
