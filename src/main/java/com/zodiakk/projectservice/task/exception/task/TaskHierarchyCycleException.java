package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class TaskHierarchyCycleException extends BusinessException {

    public TaskHierarchyCycleException() {
        super(ErrorMessages.TASK_HIERARCHY_CYCLE);
    }
}
