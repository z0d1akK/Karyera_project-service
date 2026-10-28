package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class InvalidParentTaskTypeException extends BusinessException {

    public InvalidParentTaskTypeException(String parentType, String childType) {
        super(ErrorMessages.INVALID_PARENT_TASK_TYPE.formatted(parentType, childType));
    }
}
