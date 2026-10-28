package com.zodiakk.projectservice.board.exception.column;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class BoardColumnNotFoundException extends ResourceNotFoundException {

    public BoardColumnNotFoundException(UUID id) {
        super(ErrorMessages.BOARD_COLUMN_NOT_FOUND.formatted(id));
    }
}
