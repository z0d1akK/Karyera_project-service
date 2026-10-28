package com.zodiakk.projectservice.task.exception.skill;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TaskSkillNotFoundException extends ResourceNotFoundException {

    public TaskSkillNotFoundException(UUID id) {
        super(ErrorMessages.TASK_SKILL_NOT_FOUND.formatted(id));
    }
}
