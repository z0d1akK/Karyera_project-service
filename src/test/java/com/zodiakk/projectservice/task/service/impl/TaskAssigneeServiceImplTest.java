package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.task.dto.request.action.TaskAssigneesUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.TaskAssigneeResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.exception.assignee.InvalidTaskAssigneeException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.exception.task.TaskArchivedException;
import com.zodiakk.projectservice.task.mapper.TaskAssigneeMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.service.TaskActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskAssigneeServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID KEPT_ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    private static final UUID REMOVED_ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000002");

    private static final UUID ADDED_ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000003");

    private static final UUID EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private TaskAssigneeMapper taskAssigneeMapper;

    @Mock
    private TaskActivityService taskActivityService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @InjectMocks
    private TaskAssigneeServiceImpl service;

    private final List<TaskAssignee> persisted = new ArrayList<>();

    @BeforeEach
    void setUp() {
        persisted.clear();
        lenient().when(employeeDirectoryService.getCurrentEmployee()).thenReturn(
                EmployeeReferenceDto.builder().employeeId(EMPLOYEE_ID).fullName("Ada").active(true).build()
        );
        lenient().when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenAnswer(invocation ->
                persisted.stream()
                        .filter(assignee -> assignee.getUnassignedAt() == null)
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        lenient().when(taskAssigneeRepository.saveAll(any())).thenAnswer(invocation -> {
            List<TaskAssignee> saved = new ArrayList<>();
            ((Iterable<TaskAssignee>) invocation.getArgument(0)).forEach(saved::add);
            persisted.clear();
            persisted.addAll(saved);
            return saved;
        });
        lenient().when(taskAssigneeMapper.toResponseDto(any(TaskAssignee.class))).thenAnswer(invocation -> {
            TaskAssignee assignee = invocation.getArgument(0);
            return TaskAssigneeResponseDto.builder()
                    .id(assignee.getId())
                    .taskId(assignee.getTaskId())
                    .assignmentId(assignee.getAssignmentId())
                    .assignedAt(assignee.getAssignedAt())
                    .build();
        });
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns active assignees ordered by assignment time")
        void shouldReturnAssigneesOrderedByAssignedAt() {
            persisted.add(assignee(REMOVED_ASSIGNMENT_ID, OffsetDateTime.parse("2024-02-01T00:00:00Z")));
            persisted.add(assignee(KEPT_ASSIGNMENT_ID, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(true);

            assertThat(service.findAll(TASK_ID))
                    .extracting(TaskAssigneeResponseDto::getAssignmentId)
                    .containsExactly(KEPT_ASSIGNMENT_ID, REMOVED_ASSIGNMENT_ID);
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.existsById(TASK_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.findAll(TASK_ID))
                    .isInstanceOf(ProjectTaskNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("replace")
    class Replace {

        @Test
        @DisplayName("keeps requested assignees, unassigns the rest and adds the missing ones")
        void shouldReplaceAssignees() {
            persisted.add(assignee(KEPT_ASSIGNMENT_ID, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
            persisted.add(assignee(REMOVED_ASSIGNMENT_ID, OffsetDateTime.parse("2024-02-01T00:00:00Z")));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 3L)));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    assignment(KEPT_ASSIGNMENT_ID, PROJECT_ID, null),
                    assignment(ADDED_ASSIGNMENT_ID, PROJECT_ID, LocalDate.now())
            ));

            List<TaskAssigneeResponseDto> response = service.replace(
                    TASK_ID,
                    request(List.of(ADDED_ASSIGNMENT_ID, KEPT_ASSIGNMENT_ID, KEPT_ASSIGNMENT_ID), 3L)
            );

            assertThat(response)
                    .extracting(TaskAssigneeResponseDto::getAssignmentId)
                    .containsExactly(KEPT_ASSIGNMENT_ID, ADDED_ASSIGNMENT_ID);
            assertThat(persisted)
                    .filteredOn(assignee -> REMOVED_ASSIGNMENT_ID.equals(assignee.getAssignmentId()))
                    .allSatisfy(assignee -> assertThat(assignee.getUnassignedAt()).isNotNull());
            assertThat(persisted)
                    .filteredOn(assignee -> ADDED_ASSIGNMENT_ID.equals(assignee.getAssignmentId()))
                    .allSatisfy(assignee -> {
                        assertThat(assignee.getUnassignedAt()).isNull();
                        assertThat(assignee.getAssignedAt()).isNotNull();
                        assertThat(assignee.getTaskId()).isEqualTo(TASK_ID);
                    });

            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_ASSIGNEES_CHANGED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
            verify(taskActivityService).record(
                    eq(TASK_ID),
                    eq(EMPLOYEE_ID),
                    eq("TASK_ASSIGNEES_CHANGED"),
                    any()
            );
        }

        @Test
        @DisplayName("clears every assignee when the request is empty")
        void shouldClearAssignees() {
            persisted.add(assignee(KEPT_ASSIGNMENT_ID, OffsetDateTime.parse("2024-01-01T00:00:00Z")));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 1L)));

            assertThat(service.replace(TASK_ID, request(List.of(), null))).isEmpty();

            assertThat(persisted).allSatisfy(assignee -> assertThat(assignee.getUnassignedAt()).isNotNull());
            verify(projectAssignmentRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("accepts a matching expected version")
        void shouldAcceptMatchingVersion() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 4L)));

            assertThat(service.replace(TASK_ID, request(List.of(), 4L))).isEmpty();
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.replace(TASK_ID, request(List.of(), 1L)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            verifyNoInteractions(taskAssigneeRepository, outboxEventService, taskActivityService);
        }

        @Test
        @DisplayName("fails when the task is archived")
        void shouldFailWhenTaskIsArchived() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID))
                    .thenReturn(Optional.of(task(OffsetDateTime.now(ZoneOffset.UTC), 1L)));

            assertThatThrownBy(() -> service.replace(TASK_ID, request(List.of(KEPT_ASSIGNMENT_ID), 9L)))
                    .isInstanceOf(TaskArchivedException.class);

            verifyNoInteractions(projectAssignmentRepository, outboxEventService);
        }

        @Test
        @DisplayName("fails when the expected version does not match")
        void shouldFailWhenVersionDoesNotMatch() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 2L)));

            assertThatThrownBy(() -> service.replace(TASK_ID, request(List.of(), 1L)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            verify(taskAssigneeRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("fails when an assignment cannot be found")
        void shouldFailWhenAssignmentIsMissing() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 1L)));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    assignment(KEPT_ASSIGNMENT_ID, PROJECT_ID, null)
            ));

            assertThatThrownBy(() -> service.replace(
                    TASK_ID,
                    request(List.of(KEPT_ASSIGNMENT_ID, ADDED_ASSIGNMENT_ID), 1L)
            )).isInstanceOf(InvalidTaskAssigneeException.class);
        }

        @Test
        @DisplayName("fails when the assignment belongs to another project")
        void shouldFailWhenAssignmentIsOnAnotherProject() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 1L)));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    assignment(KEPT_ASSIGNMENT_ID, OTHER_PROJECT_ID, null)
            ));

            assertThatThrownBy(() -> service.replace(TASK_ID, request(List.of(KEPT_ASSIGNMENT_ID), null)))
                    .isInstanceOf(InvalidTaskAssigneeException.class);
        }

        @Test
        @DisplayName("fails when the assignment has already ended")
        void shouldFailWhenAssignmentHasEnded() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(null, 1L)));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    assignment(KEPT_ASSIGNMENT_ID, PROJECT_ID, LocalDate.now().minusDays(1))
            ));

            assertThatThrownBy(() -> service.replace(TASK_ID, request(List.of(KEPT_ASSIGNMENT_ID), null)))
                    .isInstanceOf(InvalidTaskAssigneeException.class);

            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }
    }

    private TaskAssigneesUpdateRequestDto request(List<UUID> assignmentIds, Long version) {
        return TaskAssigneesUpdateRequestDto.builder()
                .assigneeIds(assignmentIds)
                .expectedVersion(version)
                .build();
    }

    private ProjectTask task(OffsetDateTime archivedAt, Long version) {
        return ProjectTask.builder()
                .id(TASK_ID)
                .projectId(PROJECT_ID)
                .title("Task")
                .archivedAt(archivedAt)
                .version(version)
                .build();
    }

    private TaskAssignee assignee(UUID assignmentId, OffsetDateTime assignedAt) {
        return TaskAssignee.builder()
                .taskId(TASK_ID)
                .assignmentId(assignmentId)
                .assignedAt(assignedAt)
                .build();
    }

    private ProjectAssignment assignment(UUID id, UUID projectId, LocalDate endDate) {
        return ProjectAssignment.builder()
                .id(id)
                .projectId(projectId)
                .employeeId(EMPLOYEE_ID)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(endDate)
                .build();
    }
}
