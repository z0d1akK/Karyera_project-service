package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.task.dto.request.create.TaskSkillCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskSkillResponseDto;
import com.zodiakk.projectservice.task.entity.TaskSkill;
import com.zodiakk.projectservice.task.exception.skill.TaskSkillAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.skill.TaskSkillNotFoundException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskSkillMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskSkillRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSkillServiceImplTest {

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID SKILL_ROW_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");

    private static final UUID SKILL_ID = UUID.fromString("90000000-0000-0000-0000-000000000002");

    private static final UUID EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    @Mock
    private TaskSkillRepository taskSkillRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskSkillMapper taskSkillMapper;

    @Mock
    private TaskActivityService taskActivityService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @InjectMocks
    private TaskSkillServiceImpl service;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns the skills linked to the task")
        void shouldReturnSkills() {
            TaskSkill skill = skill();
            TaskSkillResponseDto response = TaskSkillResponseDto.builder().id(SKILL_ROW_ID).build();

            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(taskSkillRepository.findAllByTaskId(TASK_ID)).thenReturn(List.of(skill));
            when(taskSkillMapper.toResponseDto(skill)).thenReturn(response);

            assertThat(service.findAll(TASK_ID)).containsExactly(response);
        }

        @Test
        @DisplayName("returns an empty list when the task has no skills")
        void shouldReturnEmptyList() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(taskSkillRepository.findAllByTaskId(TASK_ID)).thenReturn(List.of());

            assertThat(service.findAll(TASK_ID)).isEmpty();

            verifyNoInteractions(taskSkillMapper);
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.findAll(TASK_ID))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verifyNoInteractions(taskSkillRepository);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("links a skill and records activity")
        void shouldCreateSkill() {
            TaskSkillResponseDto response = TaskSkillResponseDto.builder().id(SKILL_ROW_ID).build();

            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(taskSkillRepository.existsByTaskIdAndSkillId(TASK_ID, SKILL_ID)).thenReturn(false);
            when(employeeDirectoryService.getCurrentEmployee()).thenReturn(employee());
            when(taskSkillRepository.save(any(TaskSkill.class))).thenAnswer(invocation -> {
                TaskSkill skill = invocation.getArgument(0);
                skill.setId(SKILL_ROW_ID);
                return skill;
            });
            when(taskSkillMapper.toResponseDto(any(TaskSkill.class))).thenReturn(response);

            assertThat(service.create(TASK_ID, request())).isSameAs(response);

            ArgumentCaptor<TaskSkill> captor = ArgumentCaptor.forClass(TaskSkill.class);
            verify(taskSkillRepository).save(captor.capture());
            assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
            assertThat(captor.getValue().getSkillId()).isEqualTo(SKILL_ID);
            verify(taskActivityService).record(
                    eq(TASK_ID),
                    eq(EMPLOYEE_ID),
                    eq("TASK_SKILL_ADDED"),
                    eq(java.util.Map.of("skillId", SKILL_ID))
            );
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.create(TASK_ID, request()))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verify(taskSkillRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the skill is already linked")
        void shouldFailWhenSkillAlreadyExists() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(taskSkillRepository.existsByTaskIdAndSkillId(TASK_ID, SKILL_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(TASK_ID, request()))
                    .isInstanceOf(TaskSkillAlreadyExistsException.class);

            verify(taskSkillRepository, never()).save(any());
            verifyNoInteractions(employeeDirectoryService, taskActivityService);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("removes the skill link and records activity")
        void shouldDeleteSkill() {
            TaskSkill skill = skill();
            when(taskSkillRepository.findById(SKILL_ROW_ID)).thenReturn(Optional.of(skill));
            when(employeeDirectoryService.getCurrentEmployee()).thenReturn(employee());

            service.delete(SKILL_ROW_ID);

            verify(taskSkillRepository).delete(skill);
            verify(taskActivityService).record(
                    eq(TASK_ID),
                    eq(EMPLOYEE_ID),
                    eq("TASK_SKILL_REMOVED"),
                    eq(java.util.Map.of("skillId", SKILL_ID))
            );
        }

        @Test
        @DisplayName("fails when the skill link does not exist")
        void shouldFailWhenSkillIsMissing() {
            when(taskSkillRepository.findById(SKILL_ROW_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(SKILL_ROW_ID))
                    .isInstanceOf(TaskSkillNotFoundException.class);

            verify(taskSkillRepository, never()).delete((TaskSkill) any());
            verifyNoInteractions(taskActivityService);
        }
    }

    private TaskSkillCreateRequestDto request() {
        return TaskSkillCreateRequestDto.builder().taskId(TASK_ID).skillId(SKILL_ID).build();
    }

    private TaskSkill skill() {
        return TaskSkill.builder().id(SKILL_ROW_ID).taskId(TASK_ID).skillId(SKILL_ID).build();
    }

    private EmployeeReferenceDto employee() {
        return EmployeeReferenceDto.builder().employeeId(EMPLOYEE_ID).fullName("Ada").active(true).build();
    }
}
