package com.zodiakk.projectservice.task.exception.skill;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class TaskSkillAlreadyExistsException extends BusinessException {

    public TaskSkillAlreadyExistsException(UUID skillId) {
        super(ErrorMessages.TASK_SKILL_ALREADY_EXISTS.formatted(skillId));
    }
}
