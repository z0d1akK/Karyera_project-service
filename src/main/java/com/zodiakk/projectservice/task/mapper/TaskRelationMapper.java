package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.TaskRelationCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskRelationUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskRelationResponseDto;
import com.zodiakk.projectservice.task.entity.TaskRelation;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TaskRelationMapper {

    TaskRelation toEntity(TaskRelationCreateRequestDto dto);

    TaskRelationResponseDto toResponseDto(TaskRelation entity);

    void updateEntity(TaskRelationUpdateRequestDto dto, @MappingTarget TaskRelation entity);
}
