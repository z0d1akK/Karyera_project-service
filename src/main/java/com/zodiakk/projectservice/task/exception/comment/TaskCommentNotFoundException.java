package com.zodiakk.projectservice.task.exception.comment;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TaskCommentNotFoundException extends ResourceNotFoundException {

    public TaskCommentNotFoundException(UUID id) {
        super(ErrorMessages.TASK_COMMENT_NOT_FOUND.formatted(id));
    }
}
