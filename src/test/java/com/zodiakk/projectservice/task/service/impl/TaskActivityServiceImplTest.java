package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.task.dto.response.TaskActivityResponseDto;
import com.zodiakk.projectservice.task.entity.TaskActivity;
import com.zodiakk.projectservice.task.mapper.TaskActivityMapper;
import com.zodiakk.projectservice.task.repository.TaskActivityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskActivityServiceImplTest {

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID ACTOR_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    private static final UUID ACTIVITY_ID = UUID.fromString("80000000-0000-0000-0000-000000000001");

    @Mock
    private TaskActivityRepository taskActivityRepository;

    @Mock
    private TaskActivityMapper taskActivityMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TaskActivityServiceImpl service;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("maps activities in the order returned by the repository")
        void shouldReturnMappedActivities() {
            TaskActivity first = TaskActivity.builder().taskId(TASK_ID).action("TASK_CREATED").build();
            TaskActivity second = TaskActivity.builder().taskId(TASK_ID).action("TASK_UPDATED").build();
            TaskActivityResponseDto firstDto = TaskActivityResponseDto.builder().action("TASK_CREATED").build();
            TaskActivityResponseDto secondDto = TaskActivityResponseDto.builder().action("TASK_UPDATED").build();

            when(taskActivityRepository.findAllByTaskIdOrderByCreatedAtDesc(TASK_ID))
                    .thenReturn(List.of(first, second));
            when(taskActivityMapper.toResponseDto(first)).thenReturn(firstDto);
            when(taskActivityMapper.toResponseDto(second)).thenReturn(secondDto);

            assertThat(service.findAll(TASK_ID)).containsExactly(firstDto, secondDto);
        }

        @Test
        @DisplayName("returns an empty list when the task has no activity")
        void shouldReturnEmptyList() {
            when(taskActivityRepository.findAllByTaskIdOrderByCreatedAtDesc(TASK_ID)).thenReturn(List.of());

            assertThat(service.findAll(TASK_ID)).isEmpty();

            verifyNoInteractions(taskActivityMapper);
        }
    }

    @Nested
    @DisplayName("record")
    class Record {

        @Test
        @DisplayName("persists serialized changes and returns the mapped activity")
        void shouldRecordSerializedChanges() {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("title", "Old title");
            changes.put("parentTaskId", null);
            TaskActivityResponseDto response = TaskActivityResponseDto.builder().id(ACTIVITY_ID).build();

            when(taskActivityRepository.save(any(TaskActivity.class))).thenAnswer(invocation -> {
                TaskActivity activity = invocation.getArgument(0);
                activity.setId(ACTIVITY_ID);
                return activity;
            });
            when(taskActivityMapper.toResponseDto(any(TaskActivity.class))).thenReturn(response);

            assertThat(service.record(TASK_ID, ACTOR_ID, "TASK_UPDATED", changes)).isSameAs(response);

            ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
            verify(taskActivityRepository).save(captor.capture());

            TaskActivity saved = captor.getValue();
            assertThat(saved.getTaskId()).isEqualTo(TASK_ID);
            assertThat(saved.getActorEmployeeId()).isEqualTo(ACTOR_ID);
            assertThat(saved.getAction()).isEqualTo("TASK_UPDATED");
            assertThat(saved.getChanges()).isEqualTo("{\"title\":\"Old title\",\"parentTaskId\":null}");
        }

        @Test
        @DisplayName("stores a null changes payload when no changes are supplied")
        void shouldStoreNullChanges() {
            TaskActivityResponseDto response = TaskActivityResponseDto.builder().id(ACTIVITY_ID).build();

            when(taskActivityRepository.save(any(TaskActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(taskActivityMapper.toResponseDto(any(TaskActivity.class))).thenReturn(response);

            assertThat(service.record(TASK_ID, ACTOR_ID, "TASK_ARCHIVED", null)).isSameAs(response);

            ArgumentCaptor<TaskActivity> captor = ArgumentCaptor.forClass(TaskActivity.class);
            verify(taskActivityRepository).save(captor.capture());
            assertThat(captor.getValue().getChanges()).isNull();
        }
    }
}
