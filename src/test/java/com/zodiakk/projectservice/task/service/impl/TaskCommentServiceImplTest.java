package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.task.dto.request.action.TaskCommentCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskCommentResponseDto;
import com.zodiakk.projectservice.task.entity.TaskComment;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskCommentMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskCommentRepository;
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
class TaskCommentServiceImplTest {

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID COMMENT_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");

    private static final UUID EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");

    @Mock
    private TaskCommentRepository taskCommentRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskCommentMapper taskCommentMapper;

    @Mock
    private TaskActivityService taskActivityService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @InjectMocks
    private TaskCommentServiceImpl service;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns comments mapped in repository order")
        void shouldReturnMappedComments() {
            TaskComment first = comment("First");
            TaskComment second = comment("Second");
            TaskCommentResponseDto firstDto = TaskCommentResponseDto.builder().id(COMMENT_ID).content("First").build();
            TaskCommentResponseDto secondDto = TaskCommentResponseDto.builder().content("Second").build();

            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(taskCommentRepository.findAllByTaskIdOrderByCreatedAtAsc(TASK_ID))
                    .thenReturn(List.of(first, second));
            when(taskCommentMapper.toResponseDto(first)).thenReturn(firstDto);
            when(taskCommentMapper.toResponseDto(second)).thenReturn(secondDto);

            assertThat(service.findAll(TASK_ID)).containsExactly(firstDto, secondDto);
        }

        @Test
        @DisplayName("returns an empty list when the task has no comments")
        void shouldReturnEmptyList() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(taskCommentRepository.findAllByTaskIdOrderByCreatedAtAsc(TASK_ID)).thenReturn(List.of());

            assertThat(service.findAll(TASK_ID)).isEmpty();

            verifyNoInteractions(taskCommentMapper);
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.findAll(TASK_ID))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verifyNoInteractions(taskCommentRepository);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("stores the comment for the current employee and records activity")
        void shouldCreateComment() {
            TaskCommentCreateRequestDto request = TaskCommentCreateRequestDto.builder()
                    .authorEmployeeId(OTHER_EMPLOYEE_ID)
                    .content("Looks good")
                    .build();
            TaskCommentResponseDto response = TaskCommentResponseDto.builder().id(COMMENT_ID).build();

            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);
            when(employeeDirectoryService.getCurrentEmployee()).thenReturn(employee());
            when(taskCommentRepository.save(any(TaskComment.class))).thenAnswer(invocation -> {
                TaskComment comment = invocation.getArgument(0);
                comment.setId(COMMENT_ID);
                return comment;
            });
            when(taskCommentMapper.toResponseDto(any(TaskComment.class))).thenReturn(response);

            assertThat(service.create(TASK_ID, request)).isSameAs(response);

            ArgumentCaptor<TaskComment> captor = ArgumentCaptor.forClass(TaskComment.class);
            verify(taskCommentRepository).save(captor.capture());
            assertThat(captor.getValue().getTaskId()).isEqualTo(TASK_ID);
            assertThat(captor.getValue().getAuthorEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(captor.getValue().getContent()).isEqualTo("Looks good");

            verify(taskActivityService).record(
                    eq(TASK_ID),
                    eq(EMPLOYEE_ID),
                    eq("COMMENT_ADDED"),
                    eq(java.util.Map.of("commentId", COMMENT_ID))
            );
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.create(TASK_ID, TaskCommentCreateRequestDto.builder()
                    .content("Looks good")
                    .build()))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verify(taskCommentRepository, never()).save(any());
            verifyNoInteractions(employeeDirectoryService, taskActivityService);
        }
    }

    private TaskComment comment(String content) {
        return TaskComment.builder()
                .taskId(TASK_ID)
                .authorEmployeeId(EMPLOYEE_ID)
                .content(content)
                .build();
    }

    private EmployeeReferenceDto employee() {
        return EmployeeReferenceDto.builder().employeeId(EMPLOYEE_ID).fullName("Ada").active(true).build();
    }
}
