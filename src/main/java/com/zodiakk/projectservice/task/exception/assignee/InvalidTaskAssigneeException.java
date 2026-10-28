package com.zodiakk.projectservice.task.exception.assignee;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class InvalidTaskAssigneeException extends BusinessException {

    public InvalidTaskAssigneeException(UUID assignmentId) {
        super(ErrorMessages.INVALID_TASK_ASSIGNEE.formatted(assignmentId));
    }
}
