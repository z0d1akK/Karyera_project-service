package com.zodiakk.projectservice.task.exception.activity;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TaskActivityNotFoundException extends ResourceNotFoundException {

    public TaskActivityNotFoundException(UUID id) {
        super(ErrorMessages.TASK_ACTIVITY_NOT_FOUND.formatted(id));
    }
}
