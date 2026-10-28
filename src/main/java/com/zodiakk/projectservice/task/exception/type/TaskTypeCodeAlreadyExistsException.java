package com.zodiakk.projectservice.task.exception.type;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class TaskTypeCodeAlreadyExistsException extends BusinessException {

    public TaskTypeCodeAlreadyExistsException(String code) {
        super(ErrorMessages.TASK_TYPE_CODE_ALREADY_EXISTS.formatted(code));
    }
}
