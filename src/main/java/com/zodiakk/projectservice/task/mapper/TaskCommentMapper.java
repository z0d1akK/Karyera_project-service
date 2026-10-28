package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.TaskCommentCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskCommentUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskCommentResponseDto;
import com.zodiakk.projectservice.task.entity.TaskComment;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TaskCommentMapper {

    TaskComment toEntity(TaskCommentCreateRequestDto dto);

    TaskCommentResponseDto toResponseDto(TaskComment entity);

    void updateEntity(TaskCommentUpdateRequestDto dto, @MappingTarget TaskComment entity);
}
