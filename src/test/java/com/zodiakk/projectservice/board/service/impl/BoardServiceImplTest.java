package com.zodiakk.projectservice.board.service.impl;

import com.zodiakk.projectservice.board.dto.request.action.BoardColumnOrderUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.BoardColumnUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.BoardColumnResponseDto;
import com.zodiakk.projectservice.board.dto.response.ProjectBoardResponseDto;
import com.zodiakk.projectservice.board.dto.response.TaskCardResponseDto;
import com.zodiakk.projectservice.board.entity.BoardColumn;
import com.zodiakk.projectservice.board.entity.ProjectBoard;
import com.zodiakk.projectservice.board.exception.board.ProjectBoardAlreadyExistsException;
import com.zodiakk.projectservice.board.exception.board.ProjectBoardNotFoundException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnCodeAlreadyExistsException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnInUseException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnNotFoundException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnOrderMismatchException;
import com.zodiakk.projectservice.board.exception.column.BoardColumnPositionAlreadyExistsException;
import com.zodiakk.projectservice.board.mapper.BoardColumnMapper;
import com.zodiakk.projectservice.board.mapper.ProjectBoardMapper;
import com.zodiakk.projectservice.board.repository.BoardColumnRepository;
import com.zodiakk.projectservice.board.repository.ProjectBoardRepository;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID BOARD_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");

    private static final UUID COLUMN_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_COLUMN_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private static final UUID BLOCKED_TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");

    private static final UUID TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private static final UUID ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    @Mock
    private ProjectBoardRepository projectBoardRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;

    @Mock
    private TaskRelationRepository taskRelationRepository;

    @Mock
    private TaskTypeRepository taskTypeRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectBoardMapper projectBoardMapper;

    @Mock
    private BoardColumnMapper boardColumnMapper;

    @InjectMocks
    private BoardServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(projectBoardMapper.toResponseDto(any(ProjectBoard.class))).thenAnswer(invocation -> {
            ProjectBoard board = invocation.getArgument(0);
            return ProjectBoardResponseDto.builder()
                    .id(board.getId())
                    .projectId(board.getProjectId())
                    .name(board.getName())
                    .build();
        });
        lenient().when(boardColumnMapper.toResponseDto(any(BoardColumn.class))).thenAnswer(invocation -> {
            BoardColumn column = invocation.getArgument(0);
            return BoardColumnResponseDto.builder()
                    .id(column.getId())
                    .boardId(column.getBoardId())
                    .code(column.getCode())
                    .name(column.getName())
                    .position(column.getPosition())
                    .isInitial(column.getIsInitial())
                    .isTerminal(column.getIsTerminal())
                    .build();
        });
        lenient().when(boardColumnMapper.toEntity(any(BoardColumnCreateRequestDto.class))).thenAnswer(invocation -> {
            BoardColumnCreateRequestDto request = invocation.getArgument(0);
            return BoardColumn.builder()
                    .boardId(request.getBoardId())
                    .code(request.getCode())
                    .name(request.getName())
                    .position(request.getPosition())
                    .isInitial(request.getIsInitial())
                    .isTerminal(request.getIsTerminal())
                    .build();
        });
        lenient().when(projectBoardRepository.save(any(ProjectBoard.class))).thenAnswer(invocation -> {
            ProjectBoard board = invocation.getArgument(0);
            if (board.getId() == null) {
                board.setId(BOARD_ID);
            }
            return board;
        });
        lenient().when(boardColumnRepository.save(any(BoardColumn.class))).thenAnswer(invocation -> {
            BoardColumn column = invocation.getArgument(0);
            if (column.getId() == null) {
                column.setId(COLUMN_ID);
            }
            return column;
        });
    }

    @Nested
    @DisplayName("getByProject")
    class GetByProject {

        @Test
        @DisplayName("fails when the project has no board")
        void shouldFailWhenBoardIsMissing() {
            when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByProject(PROJECT_ID))
                    .isInstanceOf(ProjectBoardNotFoundException.class);

            verifyNoInteractions(projectTaskRepository, taskTypeRepository, taskAssigneeRepository, taskRelationRepository);
        }

        @Test
        @DisplayName("returns an empty board when the project code and tasks are missing")
        void shouldReturnEmptyBoard() {
            ProjectBoard board = board("Main");
            BoardColumn column = column(COLUMN_ID, "TODO", 0, true, false);

            when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(Optional.of(board));
            when(boardColumnRepository.findAllByBoardIdOrderByPositionAsc(BOARD_ID)).thenReturn(List.of(column));
            when(projectTaskRepository.findAllByProjectIdAndArchivedAtIsNullOrderByBoardPositionAsc(PROJECT_ID))
                    .thenReturn(List.of());
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            ProjectBoardResponseDto response = service.getByProject(PROJECT_ID);

            assertThat(response.getName()).isEqualTo("Main");
            assertThat(response.getColumns()).hasSize(1);
            assertThat(response.getColumns().get(0).getTaskCount()).isZero();
            assertThat(response.getColumns().get(0).getTasks()).isEmpty();
            verify(taskTypeRepository, never()).findAllById(any());
            verify(taskAssigneeRepository, never()).findAllByTaskIdInAndUnassignedAtIsNull(any());
            verify(taskRelationRepository, never()).findAllByTargetTaskIdInAndRelationType(any(), any());
        }

        @Test
        @DisplayName("groups cards, marks unresolved blockers and leaves completed blockers clear")
        void shouldBuildColumnsWithBlockedCards() {
            ProjectBoard board = board("Main");
            BoardColumn todo = column(COLUMN_ID, "TODO", 0, true, false);
            BoardColumn done = column(OTHER_COLUMN_ID, "DONE", 1, false, true);
            ProjectTask openBlocker = task(TASK_ID, COLUMN_ID, 4L, null);
            openBlocker.setTaskTypeId(TYPE_ID);
            openBlocker.setPriority(TaskPriority.HIGH);
            ProjectTask blocked = task(BLOCKED_TASK_ID, COLUMN_ID, 5L, null);
            blocked.setTaskTypeId(null);
            ProjectTask finishedBlocker = task(OTHER_TASK_ID, OTHER_COLUMN_ID, 6L, null);
            finishedBlocker.setCompletedAt(OffsetDateTime.parse("2024-05-01T00:00:00Z"));
            finishedBlocker.setTaskTypeId(TYPE_ID);

            when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(Optional.of(board));
            when(boardColumnRepository.findAllByBoardIdOrderByPositionAsc(BOARD_ID)).thenReturn(List.of(todo, done));
            when(projectTaskRepository.findAllByProjectIdAndArchivedAtIsNullOrderByBoardPositionAsc(PROJECT_ID))
                    .thenReturn(List.of(openBlocker, blocked, finishedBlocker));
            when(taskTypeRepository.findAllById(any())).thenReturn(List.of(
                    TaskType.builder().id(TYPE_ID).code("STORY").active(true).build()
            ));
            when(taskAssigneeRepository.findAllByTaskIdInAndUnassignedAtIsNull(any())).thenReturn(List.of(
                    TaskAssignee.builder().taskId(TASK_ID).assignmentId(ASSIGNMENT_ID).build()
            ));
            when(taskRelationRepository.findAllByTargetTaskIdInAndRelationType(any(), eq(TaskRelationType.BLOCKS)))
                    .thenReturn(List.of(
                            relation(TASK_ID, BLOCKED_TASK_ID),
                            relation(OTHER_TASK_ID, TASK_ID),
                            relation(UUID.randomUUID(), BLOCKED_TASK_ID)
                    ));
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(
                    Project.builder().id(PROJECT_ID).code("PRJ").name("Project").build()
            ));

            ProjectBoardResponseDto response = service.getByProject(PROJECT_ID);

            BoardColumnResponseDto todoColumn = response.getColumns().get(0);
            assertThat(todoColumn.getTaskCount()).isEqualTo(2L);
            TaskCardResponseDto blockerCard = todoColumn.getTasks().get(0);
            assertThat(blockerCard.getKey()).isEqualTo("PRJ-4");
            assertThat(blockerCard.getType()).isEqualTo("STORY");
            assertThat(blockerCard.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(blockerCard.getBlocked()).isFalse();
            assertThat(blockerCard.getAssigneeIds()).containsExactly(ASSIGNMENT_ID);

            TaskCardResponseDto blockedCard = todoColumn.getTasks().get(1);
            assertThat(blockedCard.getBlocked()).isTrue();
            assertThat(blockedCard.getType()).isNull();
            assertThat(blockedCard.getAssigneeIds()).isEmpty();
            assertThat(blockedCard.getKey()).isEqualTo("PRJ-5");

            assertThat(response.getColumns().get(1).getTasks()).singleElement()
                    .extracting(TaskCardResponseDto::getBlocked)
                    .isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("fails when the project does not exist")
        void shouldFailWhenProjectIsMissing() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.create(PROJECT_ID, new ProjectBoardUpdateRequestDto("Board")))
                    .isInstanceOf(ProjectNotFoundException.class);

            verify(projectBoardRepository, never()).existsByProjectId(any());
        }

        @Test
        @DisplayName("fails when the project already has a board")
        void shouldFailWhenBoardAlreadyExists() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(projectBoardRepository.existsByProjectId(PROJECT_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(PROJECT_ID, new ProjectBoardUpdateRequestDto("Board")))
                    .isInstanceOf(ProjectBoardAlreadyExistsException.class);

            verify(projectBoardRepository, never()).save(any());
        }

        @Test
        @DisplayName("uses the requested name")
        void shouldUseRequestedName() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(projectBoardRepository.existsByProjectId(PROJECT_ID)).thenReturn(false);

            ProjectBoardResponseDto response = service.create(PROJECT_ID, new ProjectBoardUpdateRequestDto("Delivery"));

            assertThat(response.getName()).isEqualTo("Delivery");
            assertThat(response.getColumns()).isEmpty();
            assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
        }

        @Test
        @DisplayName("falls back to the default name when the request has no usable name")
        void shouldUseDefaultName() {
            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(projectBoardRepository.existsByProjectId(PROJECT_ID)).thenReturn(false);

            assertThat(service.create(PROJECT_ID, null).getName()).isEqualTo("Main board");
            assertThat(service.create(PROJECT_ID, new ProjectBoardUpdateRequestDto(null)).getName())
                    .isEqualTo("Main board");
            assertThat(service.create(PROJECT_ID, new ProjectBoardUpdateRequestDto("  ")).getName())
                    .isEqualTo("Main board");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("fails when the board does not exist")
        void shouldFailWhenBoardIsMissing() {
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(BOARD_ID, new ProjectBoardUpdateRequestDto("Renamed")))
                    .isInstanceOf(ProjectBoardNotFoundException.class);
        }

        @Test
        @DisplayName("updates the board and returns its columns")
        void shouldUpdateBoard() {
            ProjectBoard board = board("Main");
            BoardColumn column = column(COLUMN_ID, "TODO", 0, true, false);
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(board));
            doAnswer(invocation -> {
                board.setName("Renamed");
                return null;
            }).when(projectBoardMapper).updateEntity(any(), any());
            when(boardColumnRepository.findAllByBoardIdOrderByPositionAsc(BOARD_ID)).thenReturn(List.of(column));

            ProjectBoardResponseDto response = service.update(BOARD_ID, new ProjectBoardUpdateRequestDto("Renamed"));

            assertThat(response.getName()).isEqualTo("Renamed");
            assertThat(response.getColumns()).extracting(BoardColumnResponseDto::getCode).containsExactly("TODO");
        }
    }

    @Nested
    @DisplayName("addColumn")
    class AddColumn {

        @Test
        @DisplayName("fails when the project has no board")
        void shouldFailWhenBoardIsMissing() {
            when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addColumn(PROJECT_ID, columnRequest(0, false)))
                    .isInstanceOf(ProjectBoardNotFoundException.class);
        }

        @Test
        @DisplayName("rejects a null position")
        void shouldRejectNullPosition() {
            stubBoard();

            assertThatThrownBy(() -> service.addColumn(PROJECT_ID, columnRequest(null, false)))
                    .isInstanceOf(BoardColumnPositionAlreadyExistsException.class);
        }

        @Test
        @DisplayName("rejects a negative position")
        void shouldRejectNegativePosition() {
            stubBoard();

            assertThatThrownBy(() -> service.addColumn(PROJECT_ID, columnRequest(-1, false)))
                    .isInstanceOf(BoardColumnPositionAlreadyExistsException.class);
        }

        @Test
        @DisplayName("rejects a duplicate column code")
        void shouldRejectDuplicateCode() {
            stubBoard();
            when(boardColumnRepository.existsByBoardIdAndCodeIgnoreCase(BOARD_ID, "TODO")).thenReturn(true);

            assertThatThrownBy(() -> service.addColumn(PROJECT_ID, columnRequest(0, false)))
                    .isInstanceOf(BoardColumnCodeAlreadyExistsException.class);
        }

        @Test
        @DisplayName("rejects a duplicate position")
        void shouldRejectDuplicatePosition() {
            stubBoard();
            when(boardColumnRepository.existsByBoardIdAndCodeIgnoreCase(BOARD_ID, "TODO")).thenReturn(false);
            when(boardColumnRepository.existsByBoardIdAndPosition(BOARD_ID, 0)).thenReturn(true);

            assertThatThrownBy(() -> service.addColumn(PROJECT_ID, columnRequest(0, false)))
                    .isInstanceOf(BoardColumnPositionAlreadyExistsException.class);
        }

        @Test
        @DisplayName("clears the previous initial column when the new column is initial")
        void shouldClearPreviousInitialColumn() {
            stubBoard();
            BoardColumn previous = column(OTHER_COLUMN_ID, "BACKLOG", 1, true, false);
            allowColumn("REVIEW", 2);
            when(boardColumnRepository.findFirstByBoardIdAndIsInitialTrue(BOARD_ID)).thenReturn(Optional.of(previous));
            when(projectTaskRepository.countByColumnIdAndArchivedAtIsNull(any())).thenReturn(3L);

            BoardColumnResponseDto response = service.addColumn(PROJECT_ID, columnRequest("REVIEW", 2, true));

            assertThat(previous.getIsInitial()).isFalse();
            assertThat(response.getCode()).isEqualTo("REVIEW");
            assertThat(response.getBoardId()).isEqualTo(BOARD_ID);
            assertThat(response.getTaskCount()).isEqualTo(3L);
            assertThat(response.getTasks()).isEmpty();
            verify(boardColumnRepository).save(previous);
        }

        @Test
        @DisplayName("adds a non-initial column without touching the current initial column")
        void shouldAddColumnWithoutClearingInitial() {
            stubBoard();
            allowColumn("TODO", 0);

            BoardColumnResponseDto response = service.addColumn(PROJECT_ID, columnRequest(0, false));

            assertThat(response.getIsInitial()).isFalse();
            verify(boardColumnRepository, never()).findFirstByBoardIdAndIsInitialTrue(any());
        }

        @Test
        @DisplayName("adds an initial column when the board has none")
        void shouldAddInitialColumnWhenNoneExists() {
            stubBoard();
            allowColumn("TODO", 0);
            when(boardColumnRepository.findFirstByBoardIdAndIsInitialTrue(BOARD_ID)).thenReturn(Optional.empty());

            assertThat(service.addColumn(PROJECT_ID, columnRequest(0, true)).getIsInitial()).isTrue();

            verify(boardColumnRepository, never()).save(org.mockito.ArgumentMatchers.argThat(
                    column -> column.getId() != null && !COLUMN_ID.equals(column.getId())
            ));
        }
    }

    @Nested
    @DisplayName("updateColumn")
    class UpdateColumn {

        @Test
        @DisplayName("fails when the column does not exist")
        void shouldFailWhenColumnIsMissing() {
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateColumn(COLUMN_ID, BoardColumnUpdateRequestDto.builder().name("Doing").build()))
                    .isInstanceOf(BoardColumnNotFoundException.class);
        }

        @Test
        @DisplayName("skips the code check when the code is null, blank, or unchanged")
        void shouldSkipUnchangedCode() {
            BoardColumn column = column(COLUMN_ID, "TODO", 1, false, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));

            service.updateColumn(COLUMN_ID, BoardColumnUpdateRequestDto.builder().code(null).build());
            service.updateColumn(COLUMN_ID, BoardColumnUpdateRequestDto.builder().code("  ").build());
            service.updateColumn(COLUMN_ID, BoardColumnUpdateRequestDto.builder().code("todo").build());

            verify(boardColumnRepository, never()).existsByBoardIdAndCodeIgnoreCaseAndIdNot(any(), any(), any());
        }

        @Test
        @DisplayName("rejects a code that already belongs to another column")
        void shouldRejectDuplicateCode() {
            BoardColumn column = column(COLUMN_ID, "TODO", 1, false, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(boardColumnRepository.existsByBoardIdAndCodeIgnoreCaseAndIdNot(BOARD_ID, "DONE", COLUMN_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.updateColumn(
                    COLUMN_ID,
                    BoardColumnUpdateRequestDto.builder().code("DONE").build()
            )).isInstanceOf(BoardColumnCodeAlreadyExistsException.class);

            verify(boardColumnRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a negative position")
        void shouldRejectNegativePosition() {
            BoardColumn column = column(COLUMN_ID, "TODO", 1, false, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));

            assertThatThrownBy(() -> service.updateColumn(
                    COLUMN_ID,
                    BoardColumnUpdateRequestDto.builder().position(-1).build()
            )).isInstanceOf(BoardColumnPositionAlreadyExistsException.class);
        }

        @Test
        @DisplayName("keeps the current position even when that position is already recorded")
        void shouldKeepCurrentPosition() {
            BoardColumn column = column(COLUMN_ID, "TODO", 2, false, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));

            BoardColumnResponseDto response = service.updateColumn(
                    COLUMN_ID,
                    BoardColumnUpdateRequestDto.builder().position(2).build()
            );

            assertThat(response.getPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("rejects a position taken by another column")
        void shouldRejectTakenPosition() {
            BoardColumn column = column(COLUMN_ID, "TODO", 2, false, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(boardColumnRepository.existsByBoardIdAndPosition(BOARD_ID, 3)).thenReturn(true);

            assertThatThrownBy(() -> service.updateColumn(
                    COLUMN_ID,
                    BoardColumnUpdateRequestDto.builder().position(3).build()
            )).isInstanceOf(BoardColumnPositionAlreadyExistsException.class);
        }

        @Test
        @DisplayName("updates a column without a position and without clearing the initial flag")
        void shouldUpdateWithoutPosition() {
            BoardColumn column = column(COLUMN_ID, "TODO", 2, false, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(boardColumnRepository.existsByBoardIdAndCodeIgnoreCaseAndIdNot(BOARD_ID, "DOING", COLUMN_ID))
                    .thenReturn(false);
            stubColumnUpdate();

            BoardColumnResponseDto response = service.updateColumn(
                    COLUMN_ID,
                    BoardColumnUpdateRequestDto.builder().code("DOING").name("Doing").isInitial(false).build()
            );

            assertThat(response.getCode()).isEqualTo("DOING");
            verify(boardColumnRepository, never()).findFirstByBoardIdAndIsInitialTrue(any());
            verify(boardColumnRepository, never()).existsByBoardIdAndPosition(any(), any());
        }

        @Test
        @DisplayName("clears another initial column when this column becomes initial")
        void shouldClearOtherInitialColumn() {
            BoardColumn column = column(COLUMN_ID, "TODO", 0, false, false);
            BoardColumn previous = column(OTHER_COLUMN_ID, "BACKLOG", 1, true, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(boardColumnRepository.findFirstByBoardIdAndIsInitialTrue(BOARD_ID)).thenReturn(Optional.of(previous));
            stubColumnUpdate();

            service.updateColumn(COLUMN_ID, BoardColumnUpdateRequestDto.builder().isInitial(true).build());

            assertThat(previous.getIsInitial()).isFalse();
            verify(boardColumnRepository).save(previous);
        }

        @Test
        @DisplayName("does not clear the column when it is already the initial column")
        void shouldKeepItselfInitial() {
            BoardColumn column = column(COLUMN_ID, "TODO", 0, true, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(boardColumnRepository.findFirstByBoardIdAndIsInitialTrue(BOARD_ID)).thenReturn(Optional.of(column));

            service.updateColumn(COLUMN_ID, BoardColumnUpdateRequestDto.builder().isInitial(true).build());

            assertThat(column.getIsInitial()).isTrue();
            verify(boardColumnRepository, times(1)).save(column);
        }
    }

    @Nested
    @DisplayName("deleteColumn")
    class DeleteColumn {

        @Test
        @DisplayName("fails when the column does not exist")
        void shouldFailWhenColumnIsMissing() {
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteColumn(COLUMN_ID))
                    .isInstanceOf(BoardColumnNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the column still contains tasks")
        void shouldFailWhenColumnHasTasks() {
            BoardColumn column = column(COLUMN_ID, "TODO", 0, true, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(projectTaskRepository.countByColumnIdAndArchivedAtIsNull(COLUMN_ID)).thenReturn(2L);

            assertThatThrownBy(() -> service.deleteColumn(COLUMN_ID))
                    .isInstanceOf(BoardColumnInUseException.class);

            verify(boardColumnRepository, never()).delete((BoardColumn) any());
        }

        @Test
        @DisplayName("deletes a column that has no active tasks")
        void shouldDeleteEmptyColumn() {
            BoardColumn column = column(COLUMN_ID, "TODO", 0, true, false);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column));
            when(projectTaskRepository.countByColumnIdAndArchivedAtIsNull(COLUMN_ID)).thenReturn(0L);

            service.deleteColumn(COLUMN_ID);

            verify(boardColumnRepository).delete(column);
        }
    }

    @Nested
    @DisplayName("reorderColumns")
    class ReorderColumns {

        @Test
        @DisplayName("fails when the project has no board")
        void shouldFailWhenBoardIsMissing() {
            when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reorderColumns(PROJECT_ID, order(List.of(COLUMN_ID))))
                    .isInstanceOf(ProjectBoardNotFoundException.class);
        }

        @Test
        @DisplayName("fails when the requested ids do not match the board")
        void shouldFailWhenIdsDoNotMatch() {
            stubReorderBoard(List.of(
                    column(COLUMN_ID, "TODO", 0, true, false),
                    column(OTHER_COLUMN_ID, "DONE", 1, false, true)
            ));

            assertThatThrownBy(() -> service.reorderColumns(PROJECT_ID, order(List.of(COLUMN_ID))))
                    .isInstanceOf(BoardColumnOrderMismatchException.class);

            verify(boardColumnRepository, never()).saveAllAndFlush(any());
        }

        @Test
        @DisplayName("fails when the requested ids contain null")
        void shouldFailWhenIdsContainNull() {
            BoardColumn column = column(COLUMN_ID, "TODO", 0, true, false);
            column.setId(null);
            stubReorderBoard(List.of(column));
            List<UUID> ids = new ArrayList<>();
            ids.add(null);

            assertThatThrownBy(() -> service.reorderColumns(PROJECT_ID, order(ids)))
                    .isInstanceOf(BoardColumnOrderMismatchException.class);
        }

        @Test
        @DisplayName("reorders columns and returns them by the new positions")
        void shouldReorderColumns() {
            BoardColumn first = column(COLUMN_ID, "TODO", 5, true, false);
            BoardColumn second = column(OTHER_COLUMN_ID, "DONE", 9, false, true);
            List<BoardColumn> columns = new ArrayList<>(List.of(first, second));
            when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(Optional.of(board("Main")));
            when(boardColumnRepository.findAllByBoardIdOrderByPositionAsc(BOARD_ID)).thenAnswer(invocation ->
                    columns.stream().sorted(Comparator.comparing(BoardColumn::getPosition)).toList()
            );
            when(boardColumnRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

            ProjectBoardResponseDto response = service.reorderColumns(
                    PROJECT_ID,
                    order(List.of(OTHER_COLUMN_ID, COLUMN_ID))
            );

            assertThat(second.getPosition()).isZero();
            assertThat(first.getPosition()).isEqualTo(1);
            assertThat(response.getColumns()).extracting(BoardColumnResponseDto::getId)
                    .containsExactly(OTHER_COLUMN_ID, COLUMN_ID);
            verify(boardColumnRepository, times(2)).saveAllAndFlush(any());
        }
    }

    private void stubBoard() {
        when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                .thenReturn(Optional.of(board("Main")));
    }

    private void allowColumn(String code, int position) {
        when(boardColumnRepository.existsByBoardIdAndCodeIgnoreCase(BOARD_ID, code)).thenReturn(false);
        when(boardColumnRepository.existsByBoardIdAndPosition(BOARD_ID, position)).thenReturn(false);
    }

    private void stubReorderBoard(List<BoardColumn> columns) {
        when(projectBoardRepository.findFirstByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                .thenReturn(Optional.of(board("Main")));
        when(boardColumnRepository.findAllByBoardIdOrderByPositionAsc(BOARD_ID)).thenReturn(columns);
    }

    private void stubColumnUpdate() {
        doAnswer(invocation -> {
            BoardColumnUpdateRequestDto request = invocation.getArgument(0);
            BoardColumn column = invocation.getArgument(1);
            if (request.getCode() != null) {
                column.setCode(request.getCode());
            }
            if (request.getName() != null) {
                column.setName(request.getName());
            }
            if (request.getPosition() != null) {
                column.setPosition(request.getPosition());
            }
            if (request.getIsInitial() != null) {
                column.setIsInitial(request.getIsInitial());
            }
            if (request.getIsTerminal() != null) {
                column.setIsTerminal(request.getIsTerminal());
            }
            return null;
        }).when(boardColumnMapper).updateEntity(any(), any());
    }

    private BoardColumnOrderUpdateRequestDto order(List<UUID> columnIds) {
        return BoardColumnOrderUpdateRequestDto.builder().columnIds(columnIds).build();
    }

    private BoardColumnCreateRequestDto columnRequest(Integer position, boolean initial) {
        return columnRequest("TODO", position, initial);
    }

    private BoardColumnCreateRequestDto columnRequest(String code, Integer position, boolean initial) {
        return BoardColumnCreateRequestDto.builder()
                .code(code)
                .name(code)
                .position(position)
                .isInitial(initial)
                .isTerminal(false)
                .build();
    }

    private ProjectBoard board(String name) {
        return ProjectBoard.builder().id(BOARD_ID).projectId(PROJECT_ID).name(name).build();
    }

    private BoardColumn column(UUID id, String code, int position, boolean initial, boolean terminal) {
        return BoardColumn.builder()
                .id(id)
                .boardId(BOARD_ID)
                .code(code)
                .name(code)
                .position(position)
                .isInitial(initial)
                .isTerminal(terminal)
                .build();
    }

    private ProjectTask task(UUID id, UUID columnId, long number, OffsetDateTime completedAt) {
        return ProjectTask.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .columnId(columnId)
                .taskNumber(number)
                .title("Task " + number)
                .boardPosition(BigDecimal.valueOf(number))
                .completedAt(completedAt)
                .build();
    }

    private TaskRelation relation(UUID sourceId, UUID targetId) {
        return TaskRelation.builder()
                .sourceTaskId(sourceId)
                .targetTaskId(targetId)
                .relationType(TaskRelationType.BLOCKS)
                .build();
    }
}
