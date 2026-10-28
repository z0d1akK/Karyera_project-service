package com.zodiakk.projectservice.project.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.project.dto.request.create.ProjectMilestoneCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectMilestoneUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectMilestoneResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectMilestone;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectMilestoneMapper {

    ProjectMilestone toEntity(ProjectMilestoneCreateRequestDto dto);

    ProjectMilestoneResponseDto toResponseDto(ProjectMilestone entity);

    void updateEntity(ProjectMilestoneUpdateRequestDto dto, @MappingTarget ProjectMilestone entity);
}
