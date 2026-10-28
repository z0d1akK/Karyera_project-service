package com.zodiakk.projectservice.project.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectAssignmentUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectAssignmentResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectAssignmentMapper {

    ProjectAssignment toEntity(ProjectAssignmentCreateRequestDto dto);

    ProjectAssignmentResponseDto toResponseDto(ProjectAssignment entity);

    void updateEntity(ProjectAssignmentUpdateRequestDto dto, @MappingTarget ProjectAssignment entity);
}
