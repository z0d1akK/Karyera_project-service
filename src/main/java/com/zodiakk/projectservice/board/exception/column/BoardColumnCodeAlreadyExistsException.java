package com.zodiakk.projectservice.board.exception.column;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class BoardColumnCodeAlreadyExistsException extends BusinessException {

    public BoardColumnCodeAlreadyExistsException(String code) {
        super(ErrorMessages.BOARD_COLUMN_CODE_ALREADY_EXISTS.formatted(code));
    }
}
