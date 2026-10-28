package com.zodiakk.projectservice.board.exception.column;

import com.zodiakk.projectservice.common.exception.BusinessException;
import com.zodiakk.projectservice.common.exception.ErrorMessages;

public class BoardColumnOrderMismatchException extends BusinessException {

    public BoardColumnOrderMismatchException() {
        super(ErrorMessages.BOARD_COLUMN_ORDER_MISMATCH);
    }
}
