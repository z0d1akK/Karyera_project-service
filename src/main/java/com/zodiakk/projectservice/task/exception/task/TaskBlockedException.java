package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class TaskBlockedException extends BusinessException {

    public TaskBlockedException() {
        super(ErrorMessages.TASK_BLOCKED);
    }
}
