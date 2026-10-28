package com.zodiakk.projectservice.task.exception.type;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TaskTypeNotFoundException extends ResourceNotFoundException {

    public TaskTypeNotFoundException(UUID id) {
        super(ErrorMessages.TASK_TYPE_NOT_FOUND.formatted(id));
    }
}
