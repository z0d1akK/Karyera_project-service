package com.zodiakk.projectservice.task.exception.relation;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class TaskRelationAlreadyExistsException extends BusinessException {

    public TaskRelationAlreadyExistsException() {
        super(ErrorMessages.TASK_RELATION_ALREADY_EXISTS);
    }
}
