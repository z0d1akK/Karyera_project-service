package com.zodiakk.projectservice.project.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectRoleUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectRoleResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectRole;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ProjectRoleMapper {

    ProjectRole toEntity(ProjectRoleCreateRequestDto dto);

    ProjectRoleResponseDto toResponseDto(ProjectRole entity);

    void updateEntity(ProjectRoleUpdateRequestDto dto, @MappingTarget ProjectRole entity);
}
