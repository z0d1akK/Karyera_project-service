package com.zodiakk.projectservice.board.exception.column;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

import java.util.UUID;

public class BoardColumnInUseException extends BusinessException {

    public BoardColumnInUseException(UUID id) {
        super(ErrorMessages.BOARD_COLUMN_IN_USE.formatted(id));
    }
}
