package com.zodiakk.projectservice.task.exception.type;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class TaskTypeInUseException extends BusinessException {

    public TaskTypeInUseException(UUID id) {
        super(ErrorMessages.TASK_TYPE_IN_USE.formatted(id));
    }
}
