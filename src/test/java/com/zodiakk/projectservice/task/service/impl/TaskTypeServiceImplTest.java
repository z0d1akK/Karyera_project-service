package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.TaskTypeFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskTypeUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskTypeResponseDto;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.exception.type.TaskTypeCodeAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeInUseException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskTypeMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskTypeServiceImplTest {

    private static final UUID TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Mock
    private TaskTypeRepository taskTypeRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskTypeMapper taskTypeMapper;

    @InjectMocks
    private TaskTypeServiceImpl service;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns a page of mapped task types")
        @SuppressWarnings("unchecked")
        void shouldReturnMappedPage() {
            TaskType taskType = taskType("STORY", true);
            TaskTypeResponseDto response = TaskTypeResponseDto.builder().id(TYPE_ID).code("STORY").build();
            TaskTypeFilterDto filter = TaskTypeFilterDto.builder().active(true).build();

            when(taskTypeRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(taskType)));
            when(taskTypeMapper.toResponseDto(taskType)).thenReturn(response);

            Page<TaskTypeResponseDto> page = service.findAll(filter, PageRequestDto.builder().page(0).size(10).build());

            assertThat(page.getContent()).containsExactly(response);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns the mapped task type")
        void shouldReturnTaskType() {
            TaskType taskType = taskType("BUG", true);
            TaskTypeResponseDto response = TaskTypeResponseDto.builder().id(TYPE_ID).code("BUG").build();

            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            when(taskTypeMapper.toResponseDto(taskType)).thenReturn(response);

            assertThat(service.getById(TYPE_ID)).isSameAs(response);
        }

        @Test
        @DisplayName("fails when the task type does not exist")
        void shouldFailWhenMissing() {
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(TYPE_ID))
                    .isInstanceOf(TaskTypeNotFoundException.class);

            verifyNoInteractions(taskTypeMapper);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("defaults a missing active flag to true")
        void shouldDefaultActiveToTrue() {
            shouldCreate(null, true);
        }

        @Test
        @DisplayName("keeps an explicit active flag")
        void shouldKeepExplicitActiveFlag() {
            shouldCreate(false, false);
        }

        @Test
        @DisplayName("keeps an explicit true active flag")
        void shouldKeepActiveTrue() {
            shouldCreate(true, true);
        }

        @Test
        @DisplayName("fails when the code already exists")
        void shouldFailWhenCodeExists() {
            when(taskTypeRepository.existsByCodeIgnoreCase("STORY")).thenReturn(true);

            assertThatThrownBy(() -> service.create(createRequest("STORY", true)))
                    .isInstanceOf(TaskTypeCodeAlreadyExistsException.class);

            verify(taskTypeRepository, never()).save(any());
        }

        private void shouldCreate(Boolean requestedActive, boolean expectedActive) {
            TaskTypeResponseDto response = TaskTypeResponseDto.builder().id(TYPE_ID).code("STORY").build();

            when(taskTypeRepository.existsByCodeIgnoreCase("STORY")).thenReturn(false);
            when(taskTypeMapper.toEntity(any(TaskTypeCreateRequestDto.class))).thenAnswer(invocation -> {
                TaskTypeCreateRequestDto request = invocation.getArgument(0);
                return TaskType.builder().code(request.getCode()).name(request.getName()).build();
            });
            when(taskTypeRepository.save(any(TaskType.class))).thenAnswer(invocation -> {
                TaskType taskType = invocation.getArgument(0);
                taskType.setId(TYPE_ID);
                return taskType;
            });
            when(taskTypeMapper.toResponseDto(any(TaskType.class))).thenReturn(response);

            assertThat(service.create(createRequest("STORY", requestedActive))).isSameAs(response);

            org.mockito.ArgumentCaptor<TaskType> captor = org.mockito.ArgumentCaptor.forClass(TaskType.class);
            verify(taskTypeRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isEqualTo(expectedActive);
            assertThat(captor.getValue().getCode()).isEqualTo("STORY");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates the task type when the code is unchanged")
        void shouldUpdateWithoutCodeCheckWhenCodeMatches() {
            TaskType taskType = taskType("Story", true);
            TaskTypeResponseDto response = TaskTypeResponseDto.builder().id(TYPE_ID).build();
            TaskTypeUpdateRequestDto request = TaskTypeUpdateRequestDto.builder()
                    .code("story")
                    .name("User story")
                    .build();

            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            doAnswer(invocation -> {
                TaskTypeUpdateRequestDto update = invocation.getArgument(0);
                TaskType target = invocation.getArgument(1);
                target.setName(update.getName());
                return null;
            }).when(taskTypeMapper).updateEntity(any(), any());
            when(taskTypeRepository.save(taskType)).thenReturn(taskType);
            when(taskTypeMapper.toResponseDto(taskType)).thenReturn(response);

            assertThat(service.update(TYPE_ID, request)).isSameAs(response);

            verify(taskTypeRepository, never()).existsByCodeIgnoreCaseAndIdNot(any(), any());
        }

        @Test
        @DisplayName("skips the uniqueness check when the code is null or blank")
        void shouldSkipBlankCode() {
            TaskType taskType = taskType("STORY", true);
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            when(taskTypeRepository.save(taskType)).thenReturn(taskType);
            when(taskTypeMapper.toResponseDto(taskType)).thenReturn(TaskTypeResponseDto.builder().build());

            service.update(TYPE_ID, TaskTypeUpdateRequestDto.builder().code(null).name("Story").build());
            service.update(TYPE_ID, TaskTypeUpdateRequestDto.builder().code("  ").name("Story").build());

            verify(taskTypeRepository, never()).existsByCodeIgnoreCaseAndIdNot(any(), any());
        }

        @Test
        @DisplayName("updates the task type when the new code is free")
        void shouldUpdateWhenCodeIsFree() {
            TaskType taskType = taskType("STORY", true);
            TaskTypeResponseDto response = TaskTypeResponseDto.builder().id(TYPE_ID).code("BUG").build();

            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            when(taskTypeRepository.existsByCodeIgnoreCaseAndIdNot("BUG", TYPE_ID)).thenReturn(false);
            when(taskTypeRepository.save(taskType)).thenReturn(taskType);
            when(taskTypeMapper.toResponseDto(taskType)).thenReturn(response);

            assertThat(service.update(TYPE_ID, TaskTypeUpdateRequestDto.builder().code("BUG").build()))
                    .isSameAs(response);
        }

        @Test
        @DisplayName("fails when another task type already uses the code")
        void shouldFailWhenCodeExists() {
            TaskType taskType = taskType("STORY", true);
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            when(taskTypeRepository.existsByCodeIgnoreCaseAndIdNot("BUG", TYPE_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.update(TYPE_ID, TaskTypeUpdateRequestDto.builder().code("BUG").build()))
                    .isInstanceOf(TaskTypeCodeAlreadyExistsException.class);

            verify(taskTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("fails when the task type does not exist")
        void shouldFailWhenMissing() {
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(TYPE_ID, TaskTypeUpdateRequestDto.builder().name("Story").build()))
                    .isInstanceOf(TaskTypeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes a task type that is not used")
        void shouldDeleteUnusedType() {
            TaskType taskType = taskType("STORY", true);
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            when(projectTaskRepository.countByTaskTypeId(TYPE_ID)).thenReturn(0L);

            service.delete(TYPE_ID);

            verify(taskTypeRepository).delete(taskType);
        }

        @Test
        @DisplayName("fails when tasks still use the type")
        void shouldFailWhenTypeIsInUse() {
            TaskType taskType = taskType("STORY", true);
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(taskType));
            when(projectTaskRepository.countByTaskTypeId(TYPE_ID)).thenReturn(2L);

            assertThatThrownBy(() -> service.delete(TYPE_ID))
                    .isInstanceOf(TaskTypeInUseException.class);

            verify(taskTypeRepository, never()).delete((TaskType) any());
        }

        @Test
        @DisplayName("fails when the task type does not exist")
        void shouldFailWhenMissing() {
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(TYPE_ID))
                    .isInstanceOf(TaskTypeNotFoundException.class);

            verifyNoInteractions(projectTaskRepository);
        }
    }

    private TaskTypeCreateRequestDto createRequest(String code, Boolean active) {
        return TaskTypeCreateRequestDto.builder().code(code).name(code).active(active).build();
    }

    private TaskType taskType(String code, boolean active) {
        return TaskType.builder().id(TYPE_ID).code(code).name(code).active(active).build();
    }
}
