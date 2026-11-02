package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.task.dto.request.action.TaskRelationCreateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskRelationResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.exception.relation.CrossProjectRelationException;
import com.zodiakk.projectservice.task.exception.relation.TaskDependencyCycleException;
import com.zodiakk.projectservice.task.exception.relation.TaskRelationAlreadyExistsException;
import com.zodiakk.projectservice.task.exception.relation.TaskRelationNotFoundException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.mapper.TaskRelationMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRelationServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private static final UUID SOURCE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID TARGET_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private static final UUID MIDDLE_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");

    private static final UUID RELATION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private static final UUID EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    @Mock
    private TaskRelationRepository taskRelationRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskRelationMapper taskRelationMapper;

    @Mock
    private TaskActivityService taskActivityService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @InjectMocks
    private TaskRelationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(employeeDirectoryService.getCurrentEmployee()).thenReturn(
                EmployeeReferenceDto.builder().employeeId(EMPLOYEE_ID).fullName("Ada").active(true).build()
        );
        lenient().when(taskRelationRepository.save(any(TaskRelation.class))).thenAnswer(invocation -> {
            TaskRelation relation = invocation.getArgument(0);
            relation.setId(RELATION_ID);
            return relation;
        });
        lenient().when(taskRelationMapper.toResponseDto(any(TaskRelation.class))).thenAnswer(invocation -> {
            TaskRelation relation = invocation.getArgument(0);
            return TaskRelationResponseDto.builder()
                    .id(relation.getId())
                    .sourceTaskId(relation.getSourceTaskId())
                    .targetTaskId(relation.getTargetTaskId())
                    .relationType(relation.getRelationType())
                    .build();
        });
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns relations that start at the task")
        void shouldReturnRelations() {
            TaskRelation relation = relation(SOURCE_ID, TARGET_ID, TaskRelationType.RELATES_TO);
            TaskRelationResponseDto response = TaskRelationResponseDto.builder().id(RELATION_ID).build();

            when(projectTaskRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(taskRelationRepository.findAllBySourceTaskId(SOURCE_ID)).thenReturn(List.of(relation));
            when(taskRelationMapper.toResponseDto(relation)).thenReturn(response);

            assertThat(service.findAll(SOURCE_ID)).containsExactly(response);
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.existsById(SOURCE_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.findAll(SOURCE_ID))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verifyNoInteractions(taskRelationRepository);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates a non-blocking relation in the same project")
        void shouldCreateRelation() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, PROJECT_ID));
            when(taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                    SOURCE_ID, TARGET_ID, TaskRelationType.RELATES_TO
            )).thenReturn(false);

            TaskRelationResponseDto response = service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.RELATES_TO));

            assertThat(response.getRelationType()).isEqualTo(TaskRelationType.RELATES_TO);
            ArgumentCaptor<TaskRelation> captor = ArgumentCaptor.forClass(TaskRelation.class);
            verify(taskRelationRepository).save(captor.capture());
            assertThat(captor.getValue().getSourceTaskId()).isEqualTo(SOURCE_ID);
            assertThat(captor.getValue().getTargetTaskId()).isEqualTo(TARGET_ID);
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(EMPLOYEE_ID);
            verify(taskActivityService).record(eq(SOURCE_ID), eq(EMPLOYEE_ID), eq("TASK_RELATION_CREATED"), any());
            verify(taskRelationRepository, never()).findAllBySourceTaskIdAndRelationType(any(), any());
        }

        @Test
        @DisplayName("fails when the source task does not exist")
        void shouldFailWhenSourceIsMissing() {
            when(projectTaskRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.RELATES_TO)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verify(projectTaskRepository, never()).findById(TARGET_ID);
        }

        @Test
        @DisplayName("fails when the target task does not exist")
        void shouldFailWhenTargetIsMissing() {
            when(projectTaskRepository.findById(SOURCE_ID)).thenReturn(Optional.of(task(SOURCE_ID, PROJECT_ID)));
            when(projectTaskRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.RELATES_TO)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);
        }

        @Test
        @DisplayName("rejects a relation whose source and target are the same task")
        void shouldRejectSelfEdge() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(SOURCE_ID, PROJECT_ID));

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(SOURCE_ID, TaskRelationType.BLOCKS)))
                    .isInstanceOf(TaskRelationAlreadyExistsException.class);

            verify(taskRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a relation that crosses projects")
        void shouldRejectCrossProjectRelation() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, OTHER_PROJECT_ID));

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.DUPLICATES)))
                    .isInstanceOf(CrossProjectRelationException.class);

            verify(taskRelationRepository, never()).existsBySourceTaskIdAndTargetTaskIdAndRelationType(any(), any(), any());
        }

        @Test
        @DisplayName("rejects a duplicate source, target and type")
        void shouldRejectDuplicate() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, PROJECT_ID));
            when(taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                    SOURCE_ID, TARGET_ID, TaskRelationType.DUPLICATES
            )).thenReturn(true);

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.DUPLICATES)))
                    .isInstanceOf(TaskRelationAlreadyExistsException.class);

            verify(taskRelationRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a BLOCKS relation that closes a direct cycle")
        void shouldRejectDirectBlocksCycle() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, PROJECT_ID));
            when(taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                    SOURCE_ID, TARGET_ID, TaskRelationType.BLOCKS
            )).thenReturn(false);
            when(taskRelationRepository.findAllBySourceTaskIdAndRelationType(TARGET_ID, TaskRelationType.BLOCKS))
                    .thenReturn(List.of(relation(TARGET_ID, SOURCE_ID, TaskRelationType.BLOCKS)));

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.BLOCKS)))
                    .isInstanceOf(TaskDependencyCycleException.class);
        }

        @Test
        @DisplayName("rejects a BLOCKS relation that closes an indirect cycle")
        void shouldRejectIndirectBlocksCycle() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, PROJECT_ID));
            when(taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                    SOURCE_ID, TARGET_ID, TaskRelationType.BLOCKS
            )).thenReturn(false);
            when(taskRelationRepository.findAllBySourceTaskIdAndRelationType(TARGET_ID, TaskRelationType.BLOCKS))
                    .thenReturn(List.of(relation(TARGET_ID, MIDDLE_ID, TaskRelationType.BLOCKS)));
            when(taskRelationRepository.findAllBySourceTaskIdAndRelationType(MIDDLE_ID, TaskRelationType.BLOCKS))
                    .thenReturn(List.of(relation(MIDDLE_ID, SOURCE_ID, TaskRelationType.BLOCKS)));

            assertThatThrownBy(() -> service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.BLOCKS)))
                    .isInstanceOf(TaskDependencyCycleException.class);
        }

        @Test
        @DisplayName("allows a BLOCKS relation when an existing loop does not reach the source")
        void shouldAllowBlocksWhenVisitedLoopDoesNotReachSource() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, PROJECT_ID));
            when(taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                    SOURCE_ID, TARGET_ID, TaskRelationType.BLOCKS
            )).thenReturn(false);
            when(taskRelationRepository.findAllBySourceTaskIdAndRelationType(TARGET_ID, TaskRelationType.BLOCKS))
                    .thenReturn(List.of(relation(TARGET_ID, TARGET_ID, TaskRelationType.BLOCKS)));

            TaskRelationResponseDto response = service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.BLOCKS));

            assertThat(response.getRelationType()).isEqualTo(TaskRelationType.BLOCKS);
            verify(taskRelationRepository).save(any(TaskRelation.class));
        }

        @Test
        @DisplayName("does not run the cycle check for relations other than BLOCKS")
        void shouldSkipCycleCheckForNonBlockingRelations() {
            stubTasks(task(SOURCE_ID, PROJECT_ID), task(TARGET_ID, PROJECT_ID));
            when(taskRelationRepository.existsBySourceTaskIdAndTargetTaskIdAndRelationType(
                    SOURCE_ID, TARGET_ID, TaskRelationType.RELATES_TO
            )).thenReturn(false);

            service.create(SOURCE_ID, request(TARGET_ID, TaskRelationType.RELATES_TO));

            verify(taskRelationRepository, never())
                    .findAllBySourceTaskIdAndRelationType(any(), eq(TaskRelationType.BLOCKS));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes the relation and records activity on the source task")
        void shouldDeleteRelation() {
            TaskRelation relation = relation(SOURCE_ID, TARGET_ID, TaskRelationType.BLOCKS);
            relation.setId(RELATION_ID);
            when(taskRelationRepository.findById(RELATION_ID)).thenReturn(Optional.of(relation));

            service.delete(RELATION_ID);

            verify(taskRelationRepository).delete(relation);
            verify(taskActivityService).record(eq(SOURCE_ID), eq(EMPLOYEE_ID), eq("TASK_RELATION_DELETED"), any());
        }

        @Test
        @DisplayName("fails when the relation does not exist")
        void shouldFailWhenRelationIsMissing() {
            when(taskRelationRepository.findById(RELATION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(RELATION_ID))
                    .isInstanceOf(TaskRelationNotFoundException.class);

            verify(taskRelationRepository, never()).delete((TaskRelation) any());
            verifyNoInteractions(taskActivityService);
        }
    }

    private void stubTasks(ProjectTask source, ProjectTask target) {
        when(projectTaskRepository.findById(source.getId())).thenReturn(Optional.of(source));
        when(projectTaskRepository.findById(target.getId())).thenReturn(Optional.of(target));
    }

    private TaskRelationCreateRequestDto request(UUID targetId, TaskRelationType type) {
        return TaskRelationCreateRequestDto.builder().targetTaskId(targetId).relationType(type).build();
    }

    private ProjectTask task(UUID id, UUID projectId) {
        return ProjectTask.builder().id(id).projectId(projectId).title("Task").build();
    }

    private TaskRelation relation(UUID sourceId, UUID targetId, TaskRelationType type) {
        return TaskRelation.builder()
                .sourceTaskId(sourceId)
                .targetTaskId(targetId)
                .relationType(type)
                .createdBy(EMPLOYEE_ID)
                .build();
    }
}
