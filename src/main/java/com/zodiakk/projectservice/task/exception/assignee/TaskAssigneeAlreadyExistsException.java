package com.zodiakk.projectservice.task.exception.assignee;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class TaskAssigneeAlreadyExistsException extends BusinessException {

    public TaskAssigneeAlreadyExistsException(UUID assignmentId) {
        super(ErrorMessages.TASK_ASSIGNEE_ALREADY_EXISTS.formatted(assignmentId));
    }
}
