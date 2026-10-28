package com.zodiakk.projectservice.board.mapper;

import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.BoardColumnUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.BoardColumnResponseDto;
import com.zodiakk.projectservice.board.entity.BoardColumn;
import com.zodiakk.projectservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface BoardColumnMapper {

    BoardColumn toEntity(BoardColumnCreateRequestDto dto);

    BoardColumnResponseDto toResponseDto(BoardColumn entity);

    void updateEntity(BoardColumnUpdateRequestDto dto, @MappingTarget BoardColumn entity);
}
