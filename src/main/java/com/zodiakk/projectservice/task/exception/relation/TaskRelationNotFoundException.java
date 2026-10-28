package com.zodiakk.projectservice.task.exception.relation;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TaskRelationNotFoundException extends ResourceNotFoundException {

    public TaskRelationNotFoundException(UUID id) {
        super(ErrorMessages.TASK_RELATION_NOT_FOUND.formatted(id));
    }
}
