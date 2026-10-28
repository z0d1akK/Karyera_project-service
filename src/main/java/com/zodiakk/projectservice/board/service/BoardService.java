package com.zodiakk.projectservice.board.service;

import com.zodiakk.projectservice.board.dto.request.action.BoardColumnOrderUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.BoardColumnUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.BoardColumnResponseDto;
import com.zodiakk.projectservice.board.dto.response.ProjectBoardResponseDto;

import java.util.UUID;

public interface BoardService {

    ProjectBoardResponseDto getByProject(UUID projectId);

    ProjectBoardResponseDto create(UUID projectId, ProjectBoardUpdateRequestDto request);

    ProjectBoardResponseDto update(UUID id, ProjectBoardUpdateRequestDto request);

    BoardColumnResponseDto addColumn(UUID projectId, BoardColumnCreateRequestDto request);

    BoardColumnResponseDto updateColumn(UUID id, BoardColumnUpdateRequestDto request);

    void deleteColumn(UUID id);

    ProjectBoardResponseDto reorderColumns(UUID projectId, BoardColumnOrderUpdateRequestDto request);
}
