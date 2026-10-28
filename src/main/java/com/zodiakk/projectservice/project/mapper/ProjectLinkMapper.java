package com.zodiakk.projectservice.project.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.project.dto.request.create.ProjectLinkCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectLinkUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectLinkResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectLink;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectLinkMapper {

    ProjectLink toEntity(ProjectLinkCreateRequestDto dto);

    ProjectLinkResponseDto toResponseDto(ProjectLink entity);

    void updateEntity(ProjectLinkUpdateRequestDto dto, @MappingTarget ProjectLink entity);
}
