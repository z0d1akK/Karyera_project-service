package com.zodiakk.projectservice.task.exception.task;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class CrossProjectParentException extends BusinessException {

    public CrossProjectParentException() {
        super(ErrorMessages.CROSS_PROJECT_PARENT);
    }
}
