package com.zodiakk.projectservice.board.mapper;

import com.zodiakk.projectservice.board.dto.request.create.ProjectBoardCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.ProjectBoardResponseDto;
import com.zodiakk.projectservice.board.entity.ProjectBoard;
import com.zodiakk.projectservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectBoardMapper {

    ProjectBoard toEntity(ProjectBoardCreateRequestDto dto);

    ProjectBoardResponseDto toResponseDto(ProjectBoard entity);

    void updateEntity(ProjectBoardUpdateRequestDto dto, @MappingTarget ProjectBoard entity);
}
