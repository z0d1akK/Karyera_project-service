package com.zodiakk.projectservice.task.service.impl;

import com.zodiakk.projectservice.board.entity.BoardColumn;
import com.zodiakk.projectservice.board.entity.ProjectBoard;
import com.zodiakk.projectservice.board.exception.column.BoardColumnNotFoundException;
import com.zodiakk.projectservice.board.repository.BoardColumnRepository;
import com.zodiakk.projectservice.board.repository.ProjectBoardRepository;
import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.task.dto.request.action.TaskBoardPositionUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskParentUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.filter.ProjectTaskFilterDto;
import com.zodiakk.projectservice.task.dto.request.update.ProjectTaskUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.response.ProjectTaskResponseDto;
import com.zodiakk.projectservice.task.dto.response.TaskAssigneeResponseDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.exception.task.CrossProjectParentException;
import com.zodiakk.projectservice.task.exception.task.InvalidParentTaskTypeException;
import com.zodiakk.projectservice.task.exception.task.ProjectTaskNotFoundException;
import com.zodiakk.projectservice.task.exception.task.TaskArchivedException;
import com.zodiakk.projectservice.task.exception.task.TaskHasChildrenException;
import com.zodiakk.projectservice.task.exception.task.TaskHierarchyCycleException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeInactiveException;
import com.zodiakk.projectservice.task.exception.type.TaskTypeNotFoundException;
import com.zodiakk.projectservice.task.mapper.ProjectTaskMapper;
import com.zodiakk.projectservice.task.mapper.TaskAssigneeMapper;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectTaskServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID PARENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private static final UUID CHILD_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");

    private static final UUID ANCESTOR_ID = UUID.fromString("20000000-0000-0000-0000-000000000004");

    private static final UUID BEFORE_ID = UUID.fromString("20000000-0000-0000-0000-000000000005");

    private static final UUID AFTER_ID = UUID.fromString("20000000-0000-0000-0000-000000000006");

    private static final UUID TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private static final UUID PARENT_TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000002");

    private static final UUID CHILD_TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");

    private static final UUID COLUMN_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_COLUMN_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

    private static final UUID BOARD_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    private static final UUID EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    private static final UUID ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    private static final UUID BLOCKER_ID = UUID.fromString("20000000-0000-0000-0000-000000000007");

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private ProjectBoardRepository projectBoardRepository;

    @Mock
    private TaskTypeRepository taskTypeRepository;

    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;

    @Mock
    private TaskRelationRepository taskRelationRepository;

    @Mock
    private ProjectTaskMapper projectTaskMapper;

    @Mock
    private TaskAssigneeMapper taskAssigneeMapper;

    @Mock
    private TaskActivityService taskActivityService;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @InjectMocks
    private ProjectTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(employeeDirectoryService.getCurrentEmployee()).thenReturn(
                EmployeeReferenceDto.builder().employeeId(EMPLOYEE_ID).fullName("Ada").active(true).build()
        );
        lenient().when(projectTaskMapper.toResponseDto(any(ProjectTask.class))).thenAnswer(invocation -> {
            ProjectTask source = invocation.getArgument(0);
            return ProjectTaskResponseDto.builder().id(source.getId()).build();
        });
        lenient().when(projectTaskMapper.toEntity(any(ProjectTaskCreateRequestDto.class))).thenAnswer(invocation -> {
            ProjectTaskCreateRequestDto request = invocation.getArgument(0);
            return ProjectTask.builder()
                    .projectId(request.getProjectId())
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .taskTypeId(request.getTaskTypeId())
                    .columnId(request.getColumnId())
                    .parentTaskId(request.getParentTaskId())
                    .priority(request.getPriority())
                    .complexity(request.getComplexity())
                    .storyPoints(request.getStoryPoints())
                    .estimatedMinutes(request.getEstimatedMinutes())
                    .dueDate(request.getDueDate())
                    .build();
        });
        lenient().when(projectTaskRepository.save(any(ProjectTask.class))).thenAnswer(invocation -> {
            ProjectTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(TASK_ID);
            }
            return task;
        });
        lenient().when(projectTaskRepository.findMaxTaskNumber(PROJECT_ID)).thenReturn(7L);
        lenient().when(projectTaskRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(taskAssigneeMapper.toResponseDto(any(TaskAssignee.class))).thenAnswer(invocation -> {
            TaskAssignee assignee = invocation.getArgument(0);
            return TaskAssigneeResponseDto.builder().assignmentId(assignee.getAssignmentId()).build();
        });
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("defaults archived to false and enriches the page")
        @SuppressWarnings("unchecked")
        void shouldDefaultArchivedToFalse() {
            ProjectTask task = task(TASK_ID, "SUBTASK");
            task.setTaskNumber(8L);
            ProjectTaskFilterDto filter = ProjectTaskFilterDto.builder().build();
            when(projectTaskRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(task)));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type(TYPE_ID, "SUBTASK", true)));
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column(COLUMN_ID, false)));

            var page = service.findAll(PROJECT_ID, filter, PageRequestDto.builder().page(0).size(10).build());

            assertThat(filter.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(filter.getArchived()).isFalse();
            assertThat(page.getContent()).singleElement().satisfies(response -> {
                assertThat(response.getKey()).isEqualTo("PRJ-8");
                assertThat(response.getType()).isEqualTo("SUBTASK");
                assertThat(response.getStatus()).isEqualTo("TODO");
                assertThat(response.getBlocked()).isFalse();
            });
        }

        @Test
        @DisplayName("keeps an explicit archived flag and an empty project code")
        @SuppressWarnings("unchecked")
        void shouldKeepExplicitArchivedFlag() {
            ProjectTaskFilterDto filter = ProjectTaskFilterDto.builder().archived(true).build();
            when(projectTaskRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThat(service.findAll(PROJECT_ID, filter, PageRequestDto.builder().build()).getContent()).isEmpty();
            assertThat(filter.getArchived()).isTrue();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns counts, assignees and an unresolved blocker")
        void shouldReturnBlockedTask() {
            ProjectTask task = task(TASK_ID, "STORY");
            task.setTaskNumber(4L);
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of(
                    TaskAssignee.builder().taskId(TASK_ID).assignmentId(ASSIGNMENT_ID).build()
            ));
            when(projectTaskRepository.countByParentTaskId(TASK_ID)).thenReturn(3L);
            when(projectTaskRepository.countByParentTaskIdAndCompletedAtIsNotNull(TASK_ID)).thenReturn(1L);
            when(taskRelationRepository.findAllByTargetTaskIdInAndRelationType(List.of(TASK_ID), TaskRelationType.BLOCKS))
                    .thenReturn(List.of(TaskRelation.builder()
                            .sourceTaskId(BLOCKER_ID)
                            .targetTaskId(TASK_ID)
                            .relationType(TaskRelationType.BLOCKS)
                            .build()));
            when(projectTaskRepository.findAllById(any())).thenReturn(List.of(
                    ProjectTask.builder().id(BLOCKER_ID).completedAt(null).title("Blocker").build()
            ));

            ProjectTaskResponseDto response = service.getById(TASK_ID);

            assertThat(response.getKey()).isEqualTo("PRJ-4");
            assertThat(response.getType()).isNull();
            assertThat(response.getStatus()).isNull();
            assertThat(response.getAssignees()).extracting(TaskAssigneeResponseDto::getAssignmentId)
                    .containsExactly(ASSIGNMENT_ID);
            assertThat(response.getChildrenCount()).isEqualTo(3L);
            assertThat(response.getCompletedChildrenCount()).isEqualTo(1L);
            assertThat(response.getBlocked()).isTrue();
        }

        @Test
        @DisplayName("is not blocked by a completed predecessor")
        void shouldIgnoreCompletedBlocker() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(TASK_ID, "STORY")));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
            when(taskRelationRepository.findAllByTargetTaskIdInAndRelationType(any(), eq(TaskRelationType.BLOCKS)))
                    .thenReturn(List.of(TaskRelation.builder()
                            .sourceTaskId(BLOCKER_ID)
                            .targetTaskId(TASK_ID)
                            .relationType(TaskRelationType.BLOCKS)
                            .build()));
            when(projectTaskRepository.findAllById(any())).thenReturn(List.of(
                    ProjectTask.builder()
                            .id(BLOCKER_ID)
                            .completedAt(OffsetDateTime.parse("2024-01-01T00:00:00Z"))
                            .title("Done")
                            .build()
            ));

            assertThat(service.getById(TASK_ID).getBlocked()).isFalse();
        }

        @Test
        @DisplayName("is not blocked when the predecessor row is missing")
        void shouldIgnoreMissingBlocker() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(TASK_ID, "STORY")));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
            when(taskRelationRepository.findAllByTargetTaskIdInAndRelationType(any(), eq(TaskRelationType.BLOCKS)))
                    .thenReturn(List.of(TaskRelation.builder()
                            .sourceTaskId(BLOCKER_ID)
                            .targetTaskId(TASK_ID)
                            .relationType(TaskRelationType.BLOCKS)
                            .build()));
            when(projectTaskRepository.findAllById(any())).thenReturn(List.of());

            assertThat(service.getById(TASK_ID).getBlocked()).isFalse();
        }

        @Test
        @DisplayName("fails when the task does not exist")
        void shouldFailWhenTaskIsMissing() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(TASK_ID))
                    .isInstanceOf(ProjectTaskNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the project does not exist")
        void shouldFailWhenProjectIsMissing() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(TASK_ID, "STORY")));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(TASK_ID))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates an open task at the next number and position")
        void shouldCreateOpenTask() {
            stubProjectLock();
            stubType(TYPE_ID, "TASK", true);
            stubColumn(column(COLUMN_ID, false));
            when(projectTaskRepository.findMaxBoardPosition(COLUMN_ID)).thenReturn(new BigDecimal("1024"));

            ProjectTaskResponseDto response = service.create(PROJECT_ID, createRequest(null));

            assertThat(response.getId()).isEqualTo(TASK_ID);
            ArgumentCaptor<ProjectTask> captor = ArgumentCaptor.forClass(ProjectTask.class);
            verify(projectTaskRepository).save(captor.capture());
            ProjectTask saved = captor.getValue();
            assertThat(saved.getTaskNumber()).isEqualTo(8L);
            assertThat(saved.getBoardPosition()).isEqualByComparingTo("2048");
            assertThat(saved.getReporterEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(saved.getCompletedAt()).isNull();
            verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_CREATED"), any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("places the first card at the initial step when the column is empty")
        void shouldUseInitialPosition() {
            stubProjectLock();
            stubType(TYPE_ID, "TASK", true);
            stubColumn(column(COLUMN_ID, false));

            service.create(PROJECT_ID, createRequest(null));

            ArgumentCaptor<ProjectTask> captor = ArgumentCaptor.forClass(ProjectTask.class);
            verify(projectTaskRepository).save(captor.capture());
            assertThat(captor.getValue().getBoardPosition()).isEqualByComparingTo("1024");
        }

        @Test
        @DisplayName("completes a task created directly in a terminal column")
        void shouldCompleteTaskCreatedInTerminalColumn() {
            stubProjectLock();
            stubType(TYPE_ID, "TASK", true);
            stubColumn(column(COLUMN_ID, true));

            service.create(PROJECT_ID, createRequest(null));

            ArgumentCaptor<ProjectTask> captor = ArgumentCaptor.forClass(ProjectTask.class);
            verify(projectTaskRepository).save(captor.capture());
            assertThat(captor.getValue().getCompletedAt()).isNotNull();
            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_COMPLETED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("accepts a parent whose type allows the child, ignoring code case")
        void shouldAcceptNormalizedParentType() {
            stubProjectLock();
            stubType(TYPE_ID, "subtask", true);
            stubType(PARENT_TYPE_ID, " story ", true);
            stubColumn(column(COLUMN_ID, false));
            ProjectTask parent = task(PARENT_ID, "STORY");
            parent.setTaskTypeId(PARENT_TYPE_ID);
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            service.create(PROJECT_ID, createRequest(PARENT_ID));

            ArgumentCaptor<ProjectTask> captor = ArgumentCaptor.forClass(ProjectTask.class);
            verify(projectTaskRepository).save(captor.capture());
            assertThat(captor.getValue().getParentTaskId()).isEqualTo(PARENT_ID);
        }

        @Test
        @DisplayName("fails when the project does not exist")
        void shouldFailWhenProjectIsMissing() {
            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(ProjectNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the task type does not exist")
        void shouldFailWhenTypeIsMissing() {
            stubProjectLock();
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(TaskTypeNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the task type is inactive or has no active flag")
        void shouldFailWhenTypeIsInactive() {
            stubProjectLock();
            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type(TYPE_ID, "TASK", false)));

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(TaskTypeInactiveException.class);

            when(taskTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type(TYPE_ID, "TASK", null)));

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(TaskTypeInactiveException.class);
        }

        @Test
        @DisplayName("fails when the column or its board is missing")
        void shouldFailWhenColumnIsMissing() {
            stubProjectLock();
            stubType(TYPE_ID, "TASK", true);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(BoardColumnNotFoundException.class);

            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column(COLUMN_ID, false)));
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(BoardColumnNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the column belongs to another project")
        void shouldFailWhenColumnBelongsToAnotherProject() {
            stubProjectLock();
            stubType(TYPE_ID, "TASK", true);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column(COLUMN_ID, false)));
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(
                    ProjectBoard.builder().id(BOARD_ID).projectId(OTHER_PROJECT_ID).name("Other").build()
            ));

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(null)))
                    .isInstanceOf(BoardColumnNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the parent task does not exist")
        void shouldFailWhenParentIsMissing() {
            stubProjectLock();
            stubType(TYPE_ID, "SUBTASK", true);
            stubColumn(column(COLUMN_ID, false));
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(PARENT_ID)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the parent belongs to another project")
        void shouldFailWhenParentIsCrossProject() {
            stubProjectLock();
            stubType(TYPE_ID, "SUBTASK", true);
            stubColumn(column(COLUMN_ID, false));
            ProjectTask parent = task(PARENT_ID, "STORY");
            parent.setProjectId(OTHER_PROJECT_ID);
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(PARENT_ID)))
                    .isInstanceOf(CrossProjectParentException.class);
        }

        @Test
        @DisplayName("fails when the parent type cannot contain the child")
        void shouldFailWhenParentTypeIsInvalid() {
            stubProjectLock();
            stubType(TYPE_ID, "SUBTASK", true);
            stubType(PARENT_TYPE_ID, "EPIC", true);
            stubColumn(column(COLUMN_ID, false));
            ProjectTask parent = task(PARENT_ID, "EPIC");
            parent.setTaskTypeId(PARENT_TYPE_ID);
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            assertThatThrownBy(() -> service.create(PROJECT_ID, createRequest(PARENT_ID)))
                    .isInstanceOf(InvalidParentTaskTypeException.class)
                    .hasMessage(ErrorMessages.INVALID_PARENT_TASK_TYPE.formatted("EPIC", "SUBTASK"));
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("records changed fields and leaves equal or empty values alone")
        void shouldRecordChangedFields() {
            ProjectTask task = task(TASK_ID, "TASK");
            task.setTitle("Old");
            task.setPriority(TaskPriority.LOW);
            task.setDescription("Same");
            stubExisting(task);
            stubUpdateMapper();
            ProjectTaskUpdateRequestDto request = ProjectTaskUpdateRequestDto.builder()
                    .title("New")
                    .priority(TaskPriority.LOW)
                    .description(null)
                    .build();

            service.update(TASK_ID, request);

            assertThat(task.getTitle()).isEqualTo("New");
            Map<String, Object> changes = activityChanges();
            assertThat(changes).containsEntry("title", "Old");
            assertThat(changes).doesNotContainKey("priority");
            assertThat(changes).doesNotContainKey("description");
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("saves a task when nothing changed")
        void shouldSaveWhenNothingChanged() {
            ProjectTask task = task(TASK_ID, "TASK");
            stubExisting(task);

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().build());

            verify(projectTaskRepository).save(task);
            verifyNoInteractions(taskActivityService, outboxEventService);
        }

        @Test
        @DisplayName("publishes a complexity change")
        void shouldPublishComplexityChange() {
            ProjectTask task = task(TASK_ID, "TASK");
            task.setComplexity(TaskComplexity.LOW);
            stubExisting(task);
            doAnswer(invocation -> {
                ProjectTask target = invocation.getArgument(1);
                target.setComplexity(TaskComplexity.HIGH);
                return null;
            }).when(projectTaskMapper).updateEntity(any(), any());

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().complexity(TaskComplexity.HIGH).build());

            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_COMPLEXITY_CHANGED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("ignores a null or unchanged complexity")
        void shouldIgnoreUnchangedComplexity() {
            ProjectTask task = task(TASK_ID, "TASK");
            task.setComplexity(TaskComplexity.LOW);
            stubExisting(task);

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().complexity(null).build());
            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().complexity(TaskComplexity.LOW).build());

            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("completes a task moved onto a terminal column")
        void shouldCompleteWhenColumnBecomesTerminal() {
            ProjectTask task = task(TASK_ID, "TASK");
            stubExisting(task);
            stubColumn(column(OTHER_COLUMN_ID, true));

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().columnId(OTHER_COLUMN_ID).build());

            assertThat(task.getColumnId()).isEqualTo(OTHER_COLUMN_ID);
            assertThat(task.getCompletedAt()).isNotNull();
            assertThat(task.getBoardPosition()).isEqualByComparingTo("1024");
            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_COMPLETED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("reopens a task moved off a terminal column")
        void shouldReopenWhenColumnLeavesTerminal() {
            ProjectTask task = task(TASK_ID, "TASK");
            task.setColumnId(COLUMN_ID);
            task.setCompletedAt(OffsetDateTime.parse("2024-03-01T00:00:00Z"));
            stubExisting(task);
            stubColumn(column(COLUMN_ID, true));
            stubColumn(column(OTHER_COLUMN_ID, false));

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().columnId(OTHER_COLUMN_ID).build());

            assertThat(task.getCompletedAt()).isNull();
            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_REOPENED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("keeps completion when both columns are terminal")
        void shouldKeepCompletionBetweenTerminalColumns() {
            OffsetDateTime completedAt = OffsetDateTime.parse("2024-03-01T00:00:00Z");
            ProjectTask task = task(TASK_ID, "TASK");
            task.setCompletedAt(completedAt);
            stubExisting(task);
            stubColumn(column(COLUMN_ID, true));
            stubColumn(column(OTHER_COLUMN_ID, true));

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().columnId(OTHER_COLUMN_ID).build());

            assertThat(task.getCompletedAt()).isEqualTo(completedAt);
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("changes an active task type when existing children are allowed")
        void shouldChangeTypeWhenChildrenAreAllowed() {
            ProjectTask task = task(TASK_ID, "BUG");
            ProjectTask child = task(CHILD_ID, "SUBTASK");
            child.setTaskTypeId(CHILD_TYPE_ID);
            child.setParentTaskId(TASK_ID);
            stubExisting(task);
            stubType(PARENT_TYPE_ID, "TASK", true);
            when(projectTaskRepository.findAllByParentTaskId(TASK_ID)).thenReturn(List.of(child));
            when(taskTypeRepository.findAllById(any())).thenReturn(List.of(type(CHILD_TYPE_ID, "SUBTASK", true)));

            service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().taskTypeId(PARENT_TYPE_ID).build());

            verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_UPDATED"), any());
        }

        @Test
        @DisplayName("rejects a type change that orphans an existing child")
        void shouldRejectTypeChangeWhenChildIsIncompatible() {
            ProjectTask task = task(TASK_ID, "TASK");
            ProjectTask child = task(CHILD_ID, "SUBTASK");
            child.setTaskTypeId(CHILD_TYPE_ID);
            stubExisting(task);
            stubType(PARENT_TYPE_ID, "EPIC", true);
            when(projectTaskRepository.findAllByParentTaskId(TASK_ID)).thenReturn(List.of(child));
            when(taskTypeRepository.findAllById(any())).thenReturn(List.of(type(CHILD_TYPE_ID, "SUBTASK", true)));

            assertThatThrownBy(() -> service.update(
                    TASK_ID,
                    ProjectTaskUpdateRequestDto.builder().taskTypeId(PARENT_TYPE_ID).build()
            ))
                    .isInstanceOf(InvalidParentTaskTypeException.class)
                    .hasMessage(ErrorMessages.INVALID_PARENT_TASK_TYPE.formatted("EPIC", "SUBTASK"));
        }

        @Test
        @DisplayName("rejects a type change when a child type cannot be resolved")
        void shouldRejectTypeChangeWhenChildTypeIsMissing() {
            ProjectTask task = task(TASK_ID, "TASK");
            ProjectTask child = task(CHILD_ID, "SUBTASK");
            child.setTaskTypeId(CHILD_TYPE_ID);
            stubExisting(task);
            stubType(PARENT_TYPE_ID, "EPIC", true);
            when(projectTaskRepository.findAllByParentTaskId(TASK_ID)).thenReturn(List.of(child));
            when(taskTypeRepository.findAllById(any())).thenReturn(List.of());

            assertThatThrownBy(() -> service.update(
                    TASK_ID,
                    ProjectTaskUpdateRequestDto.builder().taskTypeId(PARENT_TYPE_ID).build()
            )).isInstanceOf(InvalidParentTaskTypeException.class);
        }

        @Test
        @DisplayName("rejects a type change that the current parent cannot contain")
        void shouldRejectTypeChangeWhenParentDisallowsIt() {
            ProjectTask task = task(TASK_ID, "STORY");
            task.setParentTaskId(PARENT_ID);
            ProjectTask parent = task(PARENT_ID, "EPIC");
            parent.setTaskTypeId(PARENT_TYPE_ID);
            stubExisting(task);
            stubType(CHILD_TYPE_ID, "SUBTASK", true);
            stubType(PARENT_TYPE_ID, "EPIC", true);
            when(projectTaskRepository.findAllByParentTaskId(TASK_ID)).thenReturn(List.of());
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            assertThatThrownBy(() -> service.update(
                    TASK_ID,
                    ProjectTaskUpdateRequestDto.builder().taskTypeId(CHILD_TYPE_ID).build()
            )).isInstanceOf(InvalidParentTaskTypeException.class);
        }

        @Test
        @DisplayName("rejects a parent that creates a hierarchy cycle")
        void shouldRejectHierarchyCycles() {
            ProjectTask task = task(TASK_ID, "STORY");
            stubExisting(task);
            stubType(TYPE_ID, "STORY", true);

            assertThatThrownBy(() -> service.update(
                    TASK_ID,
                    ProjectTaskUpdateRequestDto.builder().parentTaskId(TASK_ID).build()
            )).isInstanceOf(TaskHierarchyCycleException.class);

            ProjectTask parent = task(PARENT_ID, "EPIC");
            parent.setTaskTypeId(PARENT_TYPE_ID);
            parent.setParentTaskId(TASK_ID);
            stubType(PARENT_TYPE_ID, "EPIC", true);
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            assertThatThrownBy(() -> service.update(
                    TASK_ID,
                    ProjectTaskUpdateRequestDto.builder().parentTaskId(PARENT_ID).build()
            )).isInstanceOf(TaskHierarchyCycleException.class);
        }

        @Test
        @DisplayName("rejects a parent chain that loops")
        void shouldRejectLoopingAncestor() {
            ProjectTask task = task(TASK_ID, "SUBTASK");
            stubExisting(task);
            stubType(TYPE_ID, "SUBTASK", true);
            ProjectTask parent = task(PARENT_ID, "STORY");
            parent.setTaskTypeId(PARENT_TYPE_ID);
            parent.setParentTaskId(ANCESTOR_ID);
            ProjectTask ancestor = task(ANCESTOR_ID, "EPIC");
            ancestor.setParentTaskId(ANCESTOR_ID);
            stubType(PARENT_TYPE_ID, "STORY", true);
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
            when(projectTaskRepository.findById(ANCESTOR_ID)).thenReturn(Optional.of(ancestor));

            assertThatThrownBy(() -> service.update(
                    TASK_ID,
                    ProjectTaskUpdateRequestDto.builder().parentTaskId(PARENT_ID).build()
            )).isInstanceOf(TaskHierarchyCycleException.class);
        }

        @Test
        @DisplayName("fails when the task is missing, archived, or its project is missing")
        void shouldFailForMissingOrArchivedTask() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().build()))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            ProjectTask archived = task(TASK_ID, "TASK");
            archived.setArchivedAt(OffsetDateTime.now(ZoneOffset.UTC));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(archived));
            assertThatThrownBy(() -> service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().build()))
                    .isInstanceOf(TaskArchivedException.class);

            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(TASK_ID, "TASK")));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(TASK_ID, ProjectTaskUpdateRequestDto.builder().build()))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("archive")
    class Archive {

        @Test
        @DisplayName("archives a leaf task")
        void shouldArchiveLeaf() {
            ProjectTask task = task(TASK_ID, "TASK");
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
            when(projectTaskRepository.countByParentTaskId(TASK_ID)).thenReturn(0L);

            service.archive(TASK_ID);

            assertThat(task.getArchivedAt()).isNotNull();
            verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_ARCHIVED"), any());
        }

        @Test
        @DisplayName("fails when the task is missing, already archived, or has children")
        void shouldRejectArchive() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.archive(TASK_ID)).isInstanceOf(ProjectTaskNotFoundException.class);

            ProjectTask archived = task(TASK_ID, "TASK");
            archived.setArchivedAt(OffsetDateTime.now(ZoneOffset.UTC));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(archived));
            assertThatThrownBy(() -> service.archive(TASK_ID)).isInstanceOf(TaskArchivedException.class);

            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task(TASK_ID, "TASK")));
            when(projectTaskRepository.countByParentTaskId(TASK_ID)).thenReturn(2L);
            assertThatThrownBy(() -> service.archive(TASK_ID)).isInstanceOf(TaskHasChildrenException.class);
            verify(projectTaskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findChildren")
    class FindChildren {

        @Test
        @DisplayName("returns children ordered by board position")
        void shouldReturnChildrenInBoardOrder() {
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(task(PARENT_ID, "EPIC")));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
            ProjectTask later = task(TASK_ID, "STORY");
            later.setBoardPosition(new BigDecimal("2048"));
            later.setTaskNumber(2L);
            ProjectTask earlier = task(CHILD_ID, "TASK");
            earlier.setBoardPosition(new BigDecimal("1024"));
            earlier.setTaskNumber(1L);
            when(projectTaskRepository.findAllByParentTaskId(PARENT_ID)).thenReturn(List.of(later, earlier));

            assertThat(service.findChildren(PARENT_ID))
                    .extracting(ProjectTaskResponseDto::getId)
                    .containsExactly(CHILD_ID, TASK_ID);
        }

        @Test
        @DisplayName("fails when the parent or its project is missing")
        void shouldFailWhenParentOrProjectIsMissing() {
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findChildren(PARENT_ID))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(task(PARENT_ID, "EPIC")));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findChildren(PARENT_ID))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("changeParent")
    class ChangeParent {

        @Test
        @DisplayName("returns the task when the parent does not change")
        void shouldIgnoreSameParent() {
            ProjectTask task = task(TASK_ID, "SUBTASK");
            task.setParentTaskId(PARENT_ID);
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));

            ProjectTaskResponseDto response = service.changeParent(
                    TASK_ID,
                    TaskParentUpdateRequestDto.builder().parentTaskId(PARENT_ID).expectedVersion(1L).build()
            );

            assertThat(response.getId()).isEqualTo(TASK_ID);
            verify(projectTaskRepository, never()).save(any());
            verifyNoInteractions(taskActivityService);
        }

        @Test
        @DisplayName("detaches a task from its parent")
        void shouldDetachParent() {
            ProjectTask task = task(TASK_ID, "SUBTASK");
            task.setParentTaskId(PARENT_ID);
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));

            service.changeParent(TASK_ID, TaskParentUpdateRequestDto.builder().parentTaskId(null).expectedVersion(1L).build());

            assertThat(task.getParentTaskId()).isNull();
            verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_PARENT_CHANGED"), any());
            verify(projectTaskRepository, never()).findById(PARENT_ID);
        }

        @Test
        @DisplayName("attaches a valid parent")
        void shouldAttachParent() {
            ProjectTask task = task(TASK_ID, "SUBTASK");
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
            stubType(TYPE_ID, "SUBTASK", true);
            stubType(PARENT_TYPE_ID, "STORY", true);
            ProjectTask parent = task(PARENT_ID, "STORY");
            parent.setTaskTypeId(PARENT_TYPE_ID);
            when(projectTaskRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));

            service.changeParent(
                    TASK_ID,
                    TaskParentUpdateRequestDto.builder().parentTaskId(PARENT_ID).expectedVersion(1L).build()
            );

            assertThat(task.getParentTaskId()).isEqualTo(PARENT_ID);
            verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_PARENT_CHANGED"), any());
        }

        @Test
        @DisplayName("fails when the task is missing or archived")
        void shouldFailWhenTaskIsMissingOrArchived() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.changeParent(TASK_ID, parentRequest(null)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            ProjectTask archived = task(TASK_ID, "TASK");
            archived.setArchivedAt(OffsetDateTime.now(ZoneOffset.UTC));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(archived));
            assertThatThrownBy(() -> service.changeParent(TASK_ID, parentRequest(null)))
                    .isInstanceOf(TaskArchivedException.class);
        }
    }

    @Nested
    @DisplayName("move")
    class Move {

        @Test
        @DisplayName("places a card between two neighbours")
        void shouldPlaceBetweenNeighbours() {
            ProjectTask task = movableTask(COLUMN_ID);
            ProjectTask before = neighbour(BEFORE_ID, new BigDecimal("1024"));
            ProjectTask after = neighbour(AFTER_ID, new BigDecimal("3072"));
            stubMove(task, column(COLUMN_ID, false));

            service.move(TASK_ID, moveRequest(COLUMN_ID, BEFORE_ID, AFTER_ID));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("2048");
            verify(projectTaskRepository, never()).saveAll(any());
            verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_MOVED"), any());
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("renumbers a column when the gap is too small")
        void shouldRenumberWhenGapIsTooSmall() {
            ProjectTask task = movableTask(COLUMN_ID);
            ProjectTask before = neighbour(BEFORE_ID, new BigDecimal("10"));
            ProjectTask after = neighbour(AFTER_ID, new BigDecimal("10.0000001"));
            stubMove(task, column(COLUMN_ID, false));
            when(projectTaskRepository.findAllByColumnIdOrderByBoardPositionAsc(COLUMN_ID))
                    .thenReturn(List.of(before, after));

            service.move(TASK_ID, moveRequest(COLUMN_ID, BEFORE_ID, AFTER_ID));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("1536");
            verify(projectTaskRepository).saveAll(any());
        }

        @Test
        @DisplayName("renumbers a column when the neighbours are out of order")
        void shouldRenumberWhenNeighboursAreOutOfOrder() {
            ProjectTask task = movableTask(COLUMN_ID);
            ProjectTask before = neighbour(BEFORE_ID, new BigDecimal("3000"));
            ProjectTask after = neighbour(AFTER_ID, new BigDecimal("1000"));
            stubMove(task, column(COLUMN_ID, false));
            when(projectTaskRepository.findAllByColumnIdOrderByBoardPositionAsc(COLUMN_ID))
                    .thenReturn(List.of(after, before));

            service.move(TASK_ID, moveRequest(COLUMN_ID, BEFORE_ID, AFTER_ID));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("1536");
            verify(projectTaskRepository, times(2)).saveAll(any());
        }

        @Test
        @DisplayName("places a card before the first neighbour")
        void shouldPlaceBeforeFirstCard() {
            ProjectTask task = movableTask(COLUMN_ID);
            neighbour(AFTER_ID, new BigDecimal("2048"));
            stubMove(task, column(COLUMN_ID, false));

            service.move(TASK_ID, moveRequest(COLUMN_ID, null, AFTER_ID));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("1024");
        }

        @Test
        @DisplayName("renumbers when the following card has no room in front of it")
        void shouldRenumberWhenFollowingCardIsAtTheStart() {
            ProjectTask task = movableTask(COLUMN_ID);
            ProjectTask after = neighbour(AFTER_ID, BigDecimal.ZERO);
            stubMove(task, column(COLUMN_ID, false));
            when(projectTaskRepository.findAllByColumnIdOrderByBoardPositionAsc(COLUMN_ID)).thenReturn(List.of(after));

            service.move(TASK_ID, moveRequest(COLUMN_ID, null, AFTER_ID));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("512");
        }

        @Test
        @DisplayName("places a card after the previous neighbour")
        void shouldPlaceAfterPreviousCard() {
            ProjectTask task = movableTask(COLUMN_ID);
            neighbour(BEFORE_ID, new BigDecimal("1024"));
            stubMove(task, column(COLUMN_ID, false));

            service.move(TASK_ID, moveRequest(COLUMN_ID, BEFORE_ID, null));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("2048");
        }

        @Test
        @DisplayName("appends a card when no neighbours are given")
        void shouldAppendWhenNeighboursAreMissing() {
            ProjectTask task = movableTask(COLUMN_ID);
            stubMove(task, column(COLUMN_ID, false));
            when(projectTaskRepository.findMaxBoardPosition(COLUMN_ID)).thenReturn(new BigDecimal("4096"));

            service.move(TASK_ID, moveRequest(COLUMN_ID, TASK_ID, null));

            assertThat(task.getBoardPosition()).isEqualByComparingTo("5120");
        }

        @Test
        @DisplayName("completes a card moved onto a terminal column and keeps an existing timestamp")
        void shouldCompleteWhenMovingToTerminalColumn() {
            OffsetDateTime completedAt = OffsetDateTime.parse("2024-04-01T00:00:00Z");
            ProjectTask task = movableTask(COLUMN_ID);
            task.setCompletedAt(completedAt);
            stubMove(task, column(OTHER_COLUMN_ID, true));

            service.move(TASK_ID, moveRequest(OTHER_COLUMN_ID, null, null));

            assertThat(task.getColumnId()).isEqualTo(OTHER_COLUMN_ID);
            assertThat(task.getCompletedAt()).isEqualTo(completedAt);
            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_COMPLETED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("reopens a card moved off a terminal column")
        void shouldReopenWhenMovingOffTerminalColumn() {
            ProjectTask task = movableTask(COLUMN_ID);
            task.setCompletedAt(OffsetDateTime.parse("2024-04-01T00:00:00Z"));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
            stubColumn(column(COLUMN_ID, true));
            stubColumn(column(OTHER_COLUMN_ID, false));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));

            service.move(TASK_ID, moveRequest(OTHER_COLUMN_ID, null, null));

            assertThat(task.getCompletedAt()).isNull();
            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_REOPENED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("keeps a completed card completed when it stays on a terminal column")
        void shouldKeepCompletionOnTheSameTerminalColumn() {
            OffsetDateTime completedAt = OffsetDateTime.parse("2024-04-01T00:00:00Z");
            ProjectTask task = movableTask(COLUMN_ID);
            task.setCompletedAt(completedAt);
            stubMove(task, column(COLUMN_ID, true));

            service.move(TASK_ID, moveRequest(COLUMN_ID, null, null));

            assertThat(task.getCompletedAt()).isEqualTo(completedAt);
            verify(outboxEventService, never()).saveEvent(any(), any(), any(), anyInt(), any());
        }

        @Test
        @DisplayName("reopens an incomplete card that is already on a terminal column")
        void shouldReopenIncompleteCardOnTerminalColumn() {
            ProjectTask task = movableTask(COLUMN_ID);
            stubMove(task, column(COLUMN_ID, true));

            service.move(TASK_ID, moveRequest(COLUMN_ID, null, null));

            verify(outboxEventService).saveEvent(
                    eq(TASK_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_TASK),
                    eq(EventConstants.PROJECT_TASK_REOPENED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("fails when a neighbour is missing or sits in another column")
        void shouldRejectInvalidNeighbour() {
            ProjectTask task = movableTask(COLUMN_ID);
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
            stubColumn(column(COLUMN_ID, false));
            when(projectTaskRepository.findById(BEFORE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.move(TASK_ID, moveRequest(COLUMN_ID, BEFORE_ID, null)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            ProjectTask otherColumn = neighbour(BEFORE_ID, BigDecimal.ONE);
            otherColumn.setColumnId(OTHER_COLUMN_ID);
            when(projectTaskRepository.findById(BEFORE_ID)).thenReturn(Optional.of(otherColumn));

            assertThatThrownBy(() -> service.move(TASK_ID, moveRequest(COLUMN_ID, BEFORE_ID, null)))
                    .isInstanceOf(BoardColumnNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the task is missing, archived, or the target column is foreign")
        void shouldRejectMove() {
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.move(TASK_ID, moveRequest(COLUMN_ID, null, null)))
                    .isInstanceOf(ProjectTaskNotFoundException.class);

            ProjectTask archived = movableTask(COLUMN_ID);
            archived.setArchivedAt(OffsetDateTime.now(ZoneOffset.UTC));
            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(archived));
            assertThatThrownBy(() -> service.move(TASK_ID, moveRequest(COLUMN_ID, null, null)))
                    .isInstanceOf(TaskArchivedException.class);

            when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(movableTask(COLUMN_ID)));
            when(boardColumnRepository.findById(OTHER_COLUMN_ID)).thenReturn(Optional.of(column(OTHER_COLUMN_ID, false)));
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(
                    ProjectBoard.builder().id(BOARD_ID).projectId(OTHER_PROJECT_ID).name("Other").build()
            ));
            assertThatThrownBy(() -> service.move(TASK_ID, moveRequest(OTHER_COLUMN_ID, null, null)))
                    .isInstanceOf(BoardColumnNotFoundException.class);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> activityChanges() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(taskActivityService).record(eq(TASK_ID), eq(EMPLOYEE_ID), eq("TASK_UPDATED"), captor.capture());
        return captor.getValue();
    }

    private void stubExisting(ProjectTask task) {
        when(projectTaskRepository.findByIdForUpdate(task.getId())).thenReturn(Optional.of(task));
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
    }

    private void stubMove(ProjectTask task, BoardColumn target) {
        when(projectTaskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
        stubColumn(target);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
    }

    private void stubProjectLock() {
        when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project("PRJ")));
    }

    private void stubType(UUID id, String code, Boolean active) {
        when(taskTypeRepository.findById(id)).thenReturn(Optional.of(type(id, code, active)));
    }

    private void stubColumn(BoardColumn column) {
        when(boardColumnRepository.findById(column.getId())).thenReturn(Optional.of(column));
        when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(
                ProjectBoard.builder().id(BOARD_ID).projectId(PROJECT_ID).name("Main").build()
        ));
    }

    private void stubUpdateMapper() {
        doAnswer(invocation -> {
            ProjectTaskUpdateRequestDto request = invocation.getArgument(0);
            ProjectTask target = invocation.getArgument(1);
            if (request.getTitle() != null) {
                target.setTitle(request.getTitle());
            }
            if (request.getDescription() != null) {
                target.setDescription(request.getDescription());
            }
            if (request.getPriority() != null) {
                target.setPriority(request.getPriority());
            }
            if (request.getTaskTypeId() != null) {
                target.setTaskTypeId(request.getTaskTypeId());
            }
            if (request.getParentTaskId() != null) {
                target.setParentTaskId(request.getParentTaskId());
            }
            if (request.getComplexity() != null) {
                target.setComplexity(request.getComplexity());
            }
            if (request.getStoryPoints() != null) {
                target.setStoryPoints(request.getStoryPoints());
            }
            if (request.getEstimatedMinutes() != null) {
                target.setEstimatedMinutes(request.getEstimatedMinutes());
            }
            if (request.getDueDate() != null) {
                target.setDueDate(request.getDueDate());
            }
            return null;
        }).when(projectTaskMapper).updateEntity(any(), any());
    }

    private ProjectTaskCreateRequestDto createRequest(UUID parentId) {
        return ProjectTaskCreateRequestDto.builder()
                .title("Write tests")
                .taskTypeId(TYPE_ID)
                .columnId(COLUMN_ID)
                .parentTaskId(parentId)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.of(2024, 8, 1))
                .build();
    }

    private TaskParentUpdateRequestDto parentRequest(UUID parentId) {
        return TaskParentUpdateRequestDto.builder().parentTaskId(parentId).expectedVersion(1L).build();
    }

    private TaskBoardPositionUpdateRequestDto moveRequest(UUID columnId, UUID beforeId, UUID afterId) {
        return TaskBoardPositionUpdateRequestDto.builder()
                .targetColumnId(columnId)
                .beforeTaskId(beforeId)
                .afterTaskId(afterId)
                .expectedVersion(1L)
                .build();
    }

    private ProjectTask movableTask(UUID columnId) {
        ProjectTask task = task(TASK_ID, "TASK");
        task.setColumnId(columnId);
        task.setBoardPosition(new BigDecimal("100"));
        return task;
    }

    private ProjectTask neighbour(UUID id, BigDecimal position) {
        ProjectTask neighbour = task(id, "TASK");
        neighbour.setColumnId(COLUMN_ID);
        neighbour.setBoardPosition(position);
        when(projectTaskRepository.findById(id)).thenReturn(Optional.of(neighbour));
        return neighbour;
    }

    private ProjectTask task(UUID id, String typeCode) {
        return ProjectTask.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .taskNumber(1L)
                .title(typeCode + " task")
                .taskTypeId(TYPE_ID)
                .columnId(COLUMN_ID)
                .priority(TaskPriority.MEDIUM)
                .boardPosition(BigDecimal.ONE)
                .build();
    }

    private TaskType type(UUID id, String code, Boolean active) {
        return TaskType.builder().id(id).code(code).name(code).active(active).build();
    }

    private BoardColumn column(UUID id, boolean terminal) {
        return BoardColumn.builder()
                .id(id)
                .boardId(BOARD_ID)
                .code(terminal ? "DONE" : "TODO")
                .name(terminal ? "Done" : "To do")
                .position(terminal ? 1 : 0)
                .isInitial(!terminal)
                .isTerminal(terminal)
                .build();
    }

    private Project project(String code) {
        return Project.builder().id(PROJECT_ID).code(code).name("Project").build();
    }
}
