package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class TaskArchivedException extends BusinessException {

    public TaskArchivedException(UUID id) {
        super(ErrorMessages.TASK_ARCHIVED.formatted(id));
    }
}
