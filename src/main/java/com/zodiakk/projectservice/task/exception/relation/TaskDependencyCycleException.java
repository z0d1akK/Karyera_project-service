package com.zodiakk.projectservice.task.exception.relation;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class TaskDependencyCycleException extends BusinessException {

    public TaskDependencyCycleException() {
        super(ErrorMessages.TASK_DEPENDENCY_CYCLE);
    }
}
