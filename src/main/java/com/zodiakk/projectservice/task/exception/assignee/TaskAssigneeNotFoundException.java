package com.zodiakk.projectservice.task.exception.assignee;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TaskAssigneeNotFoundException extends ResourceNotFoundException {

    public TaskAssigneeNotFoundException(UUID id) {
        super(ErrorMessages.TASK_ASSIGNEE_NOT_FOUND.formatted(id));
    }
}
