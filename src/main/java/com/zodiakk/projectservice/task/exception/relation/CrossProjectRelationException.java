package com.zodiakk.projectservice.task.exception.relation;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class CrossProjectRelationException extends BusinessException {

    public CrossProjectRelationException() {
        super(ErrorMessages.CROSS_PROJECT_RELATION);
    }
}
