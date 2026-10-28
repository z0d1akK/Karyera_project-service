package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class TaskHasChildrenException extends BusinessException {

    public TaskHasChildrenException(UUID id) {
        super(ErrorMessages.TASK_HAS_CHILDREN.formatted(id));
    }
}
