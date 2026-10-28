package com.zodiakk.projectservice.task.exception.type;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class TaskTypeInactiveException extends BusinessException {

    public TaskTypeInactiveException(UUID id) {
        super(ErrorMessages.TASK_TYPE_INACTIVE.formatted(id));
    }
}
