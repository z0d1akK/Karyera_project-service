package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectTaskNotFoundException extends ResourceNotFoundException {

    public ProjectTaskNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_TASK_NOT_FOUND.formatted(id));
    }
}
