package com.zodiakk.projectservice.board.exception.board;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class ProjectBoardAlreadyExistsException extends BusinessException {

    public ProjectBoardAlreadyExistsException(UUID projectId) {
        super(ErrorMessages.PROJECT_BOARD_ALREADY_EXISTS.formatted(projectId));
    }
}
