package com.zodiakk.projectservice.task.mapper;

import com.zodiakk.projectservice.config.MapStructConfig;
import com.zodiakk.projectservice.task.dto.request.create.TaskSkillCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskSkillUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskSkillResponseDto;
import com.zodiakk.projectservice.task.entity.TaskSkill;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TaskSkillMapper {

    TaskSkill toEntity(TaskSkillCreateRequestDto dto);

    TaskSkillResponseDto toResponseDto(TaskSkill entity);

    void updateEntity(TaskSkillUpdateRequestDto dto, @MappingTarget TaskSkill entity);
}
