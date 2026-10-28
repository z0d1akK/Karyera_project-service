package com.zodiakk.projectservice.project.exception.assignment;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.math.BigDecimal;

public class InvalidAllocationPercentException extends BusinessException {

    public InvalidAllocationPercentException(BigDecimal value) {
        super(ErrorMessages.INVALID_ALLOCATION_PERCENT.formatted(value));
    }
}
