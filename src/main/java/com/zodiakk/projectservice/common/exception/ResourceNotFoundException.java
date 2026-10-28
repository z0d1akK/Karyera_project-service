package com.zodiakk.projectservice.common.exception;

public abstract class ResourceNotFoundException extends BusinessException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
