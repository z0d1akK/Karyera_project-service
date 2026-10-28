package com.zodiakk.projectservice.board.exception.board;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class ProjectBoardNotFoundException extends ResourceNotFoundException {

    public ProjectBoardNotFoundException(UUID id) {
        super(ErrorMessages.PROJECT_BOARD_NOT_FOUND.formatted(id));
    }
}
