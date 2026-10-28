package com.zodiakk.projectservice.board.exception.column;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class BoardColumnPositionAlreadyExistsException extends BusinessException {

    public BoardColumnPositionAlreadyExistsException(Integer position) {
        super(ErrorMessages.BOARD_COLUMN_POSITION_ALREADY_EXISTS.formatted(position));
    }
}
