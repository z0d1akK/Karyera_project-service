package com.zodiakk.projectservice.internal.service.impl;

import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.internal.dto.MemberEligibilityResponseDto;
import com.zodiakk.projectservice.internal.dto.ProjectHistoryResponseDto;
import com.zodiakk.projectservice.internal.dto.TaskCompletionEvidenceResponseDto;
import com.zodiakk.projectservice.internal.dto.TaskStatisticsResponseDto;
import com.zodiakk.projectservice.internal.dto.WorkEligibilityReasonCode;
import com.zodiakk.projectservice.internal.dto.WorkEligibilityResponseDto;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.entity.ProjectRole;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.entity.TaskType;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalProjectServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");

    private static final UUID EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");

    private static final UUID TASK_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private static final UUID ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    private static final UUID OTHER_ASSIGNMENT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000002");

    private static final UUID ROLE_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    private static final UUID TYPE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private static final LocalDate WORK_DATE = LocalDate.of(2024, 6, 15);

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskTypeRepository taskTypeRepository;

    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;

    @InjectMocks
    private InternalProjectServiceImpl service;

    @Nested
    @DisplayName("getWorkEligibility")
    class GetWorkEligibility {

        @Test
        @DisplayName("reports a missing project")
        void shouldReportMissingProject() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            WorkEligibilityResponseDto response = service.getWorkEligibility(
                    PROJECT_ID, EMPLOYEE_ID, TASK_ID, WORK_DATE
            );

            assertThat(response.eligible()).isFalse();
            assertThat(response.reasonCode()).isEqualTo(WorkEligibilityReasonCode.PROJECT_NOT_FOUND);
            assertThat(response.projectActive()).isFalse();
            assertThat(response.assignmentActiveOnDate()).isFalse();
            assertThat(response.projectAssignmentId()).isNull();
            assertThat(response.taskBelongsToProject()).isNull();
            assertThat(response.taskArchived()).isNull();
            verifyNoInteractions(projectAssignmentRepository, projectTaskRepository);
        }

        @Test
        @DisplayName("reports a missing task before the other checks")
        void shouldReportMissingTask() {
            stubProject(ProjectStatus.ACTIVE, null, null);
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE))
                    .thenReturn(List.of(assignment(ASSIGNMENT_ID, PROJECT_ID, WORK_DATE.minusDays(1), null)));
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            WorkEligibilityResponseDto response = eligibility(TASK_ID);

            assertThat(response.eligible()).isFalse();
            assertThat(response.reasonCode()).isEqualTo(WorkEligibilityReasonCode.TASK_NOT_FOUND);
            assertThat(response.projectAssignmentId()).isEqualTo(ASSIGNMENT_ID);
            assertThat(response.projectActive()).isTrue();
            assertThat(response.assignmentActiveOnDate()).isTrue();
            assertThat(response.taskBelongsToProject()).isFalse();
            assertThat(response.taskArchived()).isNull();
        }

        @Test
        @DisplayName("reports a task that belongs to another project")
        void shouldReportTaskProjectMismatch() {
            stubProject(ProjectStatus.ACTIVE, null, null);
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE)).thenReturn(List.of());
            ProjectTask task = task(OTHER_PROJECT_ID, null, null);
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

            WorkEligibilityResponseDto response = eligibility(TASK_ID);

            assertThat(response.reasonCode()).isEqualTo(WorkEligibilityReasonCode.TASK_PROJECT_MISMATCH);
            assertThat(response.taskBelongsToProject()).isFalse();
            assertThat(response.taskArchived()).isFalse();
            assertThat(response.eligible()).isFalse();
        }

        @Test
        @DisplayName("rejects planned and on-hold projects for time tracking")
        void shouldRejectProjectsThatAreNotHistoricallyAvailable() {
            stubProject(ProjectStatus.PLANNED, WORK_DATE.minusDays(10), WORK_DATE.plusDays(10));

            assertThat(eligibility(null).reasonCode())
                    .isEqualTo(WorkEligibilityReasonCode.PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING);

            stubProject(ProjectStatus.ON_HOLD, null, null);

            assertThat(eligibility(null).reasonCode())
                    .isEqualTo(WorkEligibilityReasonCode.PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING);
        }

        @Test
        @DisplayName("rejects a closed project outside its historical window")
        void shouldRejectClosedProjectOutsideItsWindow() {
            stubProject(ProjectStatus.COMPLETED, WORK_DATE.minusDays(10), null);
            assertThat(eligibility(null).reasonCode())
                    .isEqualTo(WorkEligibilityReasonCode.PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING);

            stubProject(ProjectStatus.CANCELLED, WORK_DATE.minusDays(10), WORK_DATE.minusDays(1));
            assertThat(eligibility(null).reasonCode())
                    .isEqualTo(WorkEligibilityReasonCode.PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING);

            stubProject(ProjectStatus.COMPLETED, WORK_DATE.plusDays(1), WORK_DATE.plusDays(10));
            assertThat(eligibility(null).reasonCode())
                    .isEqualTo(WorkEligibilityReasonCode.PROJECT_NOT_AVAILABLE_FOR_TIME_TRACKING);
        }

        @Test
        @DisplayName("accepts an active project and a closed project inside its historical window")
        void shouldAcceptAvailableProjects() {
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE))
                    .thenReturn(List.of(assignment(ASSIGNMENT_ID, PROJECT_ID, WORK_DATE, WORK_DATE)));

            stubProject(ProjectStatus.ACTIVE, WORK_DATE.plusDays(30), null);
            WorkEligibilityResponseDto active = eligibility(null);
            assertThat(active.eligible()).isTrue();
            assertThat(active.reasonCode()).isNull();
            assertThat(active.projectActive()).isTrue();
            assertThat(active.taskBelongsToProject()).isNull();
            assertThat(active.taskArchived()).isNull();
            assertThat(active.projectAssignmentId()).isEqualTo(ASSIGNMENT_ID);

            stubProject(ProjectStatus.COMPLETED, null, WORK_DATE);
            WorkEligibilityResponseDto completed = eligibility(null);
            assertThat(completed.eligible()).isTrue();
            assertThat(completed.projectActive()).isFalse();

            stubProject(ProjectStatus.CANCELLED, WORK_DATE, WORK_DATE);
            assertThat(eligibility(null).eligible()).isTrue();
        }

        @Test
        @DisplayName("ignores an active assignment on another project")
        void shouldIgnoreAssignmentOnAnotherProject() {
            stubProject(ProjectStatus.ACTIVE, null, null);
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE)).thenReturn(List.of(
                    assignment(OTHER_ASSIGNMENT_ID, OTHER_PROJECT_ID, WORK_DATE.minusDays(1), null)
            ));

            WorkEligibilityResponseDto response = eligibility(null);

            assertThat(response.reasonCode()).isEqualTo(WorkEligibilityReasonCode.NO_ACTIVE_PROJECT_ASSIGNMENT);
            assertThat(response.assignmentActiveOnDate()).isFalse();
            assertThat(response.projectAssignmentId()).isNull();
            assertThat(response.projectActive()).isTrue();
        }

        @Test
        @DisplayName("reports an archived task after the assignment check")
        void shouldReportArchivedTask() {
            stubProject(ProjectStatus.ACTIVE, null, null);
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, WORK_DATE, null)
            ));
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(
                    task(PROJECT_ID, OffsetDateTime.parse("2024-01-01T00:00:00Z"), null)
            ));

            WorkEligibilityResponseDto response = eligibility(TASK_ID);

            assertThat(response.reasonCode()).isEqualTo(WorkEligibilityReasonCode.TASK_ARCHIVED);
            assertThat(response.taskBelongsToProject()).isTrue();
            assertThat(response.taskArchived()).isTrue();
            assertThat(response.eligible()).isFalse();
        }

        @Test
        @DisplayName("reports a missing assignment before an archived task")
        void shouldPreferMissingAssignmentOverArchivedTask() {
            stubProject(ProjectStatus.ACTIVE, null, null);
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE)).thenReturn(List.of());
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(
                    task(PROJECT_ID, OffsetDateTime.parse("2024-01-01T00:00:00Z"), null)
            ));

            assertThat(eligibility(TASK_ID).reasonCode())
                    .isEqualTo(WorkEligibilityReasonCode.NO_ACTIVE_PROJECT_ASSIGNMENT);
        }

        @Test
        @DisplayName("accepts an open task on an available project with an active assignment")
        void shouldAcceptEligibleWork() {
            stubProject(ProjectStatus.COMPLETED, WORK_DATE.minusDays(5), WORK_DATE);
            when(projectAssignmentRepository.findActiveOnDate(EMPLOYEE_ID, WORK_DATE)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, WORK_DATE.minusDays(1), null)
            ));
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(PROJECT_ID, null, null)));

            WorkEligibilityResponseDto response = eligibility(TASK_ID);

            assertThat(response.eligible()).isTrue();
            assertThat(response.reasonCode()).isNull();
            assertThat(response.projectActive()).isFalse();
            assertThat(response.taskBelongsToProject()).isTrue();
            assertThat(response.taskArchived()).isFalse();
            assertThat(response.assignmentActiveOnDate()).isTrue();
            assertThat(response.workDate()).isEqualTo(WORK_DATE);
            assertThat(response.employeeId()).isEqualTo(EMPLOYEE_ID);
        }
    }

    @Nested
    @DisplayName("getProjectHistory")
    class GetProjectHistory {

        @Test
        @DisplayName("returns an empty history when the employee has no assignments")
        void shouldReturnEmptyHistory() {
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());

            ProjectHistoryResponseDto response = service.getProjectHistory(EMPLOYEE_ID);

            assertThat(response.employeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(response.projects()).isEmpty();
            verify(projectRepository, never()).findAllById(any());
            verify(projectRoleRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("returns newest assignments first and fills project and role names")
        void shouldReturnNewestAssignmentsFirst() {
            ProjectAssignment older = assignment(ASSIGNMENT_ID, PROJECT_ID, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1));
            older.setProjectRoleId(ROLE_ID);
            older.setAllocationPercent(new BigDecimal("50"));
            ProjectAssignment newer = assignment(OTHER_ASSIGNMENT_ID, OTHER_PROJECT_ID, LocalDate.of(2024, 6, 1), null);
            newer.setProjectRoleId(ROLE_ID);
            newer.setAllocationPercent(new BigDecimal("100"));

            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(older, newer));
            when(projectRepository.findAllById(any())).thenReturn(List.of(
                    Project.builder().id(PROJECT_ID).code("OLD").name("Old").status(ProjectStatus.COMPLETED).build(),
                    Project.builder().id(OTHER_PROJECT_ID).code("NEW").name("New").status(ProjectStatus.ACTIVE).build()
            ));
            when(projectRoleRepository.findAllById(any())).thenReturn(List.of(
                    ProjectRole.builder().id(ROLE_ID).code("DEV").name("Developer").active(true).build()
            ));

            ProjectHistoryResponseDto response = service.getProjectHistory(EMPLOYEE_ID);

            assertThat(response.projects()).hasSize(2);
            assertThat(response.projects().get(0).projectCode()).isEqualTo("NEW");
            assertThat(response.projects().get(0).projectRoleName()).isEqualTo("Developer");
            assertThat(response.projects().get(0).active()).isTrue();
            assertThat(response.projects().get(0).allocationPercent()).isEqualByComparingTo("100");
            assertThat(response.projects().get(1).projectCode()).isEqualTo("OLD");
            assertThat(response.projects().get(1).projectStatus()).isEqualTo(ProjectStatus.COMPLETED);
            assertThat(response.projects().get(1).active()).isFalse();
        }

        @Test
        @DisplayName("keeps null project and role references when they cannot be loaded")
        void shouldKeepMissingReferences() {
            ProjectAssignment assignment = assignment(ASSIGNMENT_ID, PROJECT_ID, null, null);
            assignment.setProjectRoleId(ROLE_ID);
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(assignment));
            when(projectRepository.findAllById(any())).thenReturn(List.of());
            when(projectRoleRepository.findAllById(any())).thenReturn(List.of());

            var entry = service.getProjectHistory(EMPLOYEE_ID).projects().get(0);

            assertThat(entry.projectCode()).isNull();
            assertThat(entry.projectName()).isNull();
            assertThat(entry.projectStatus()).isNull();
            assertThat(entry.projectRoleName()).isNull();
            assertThat(entry.active()).isTrue();
            assertThat(entry.startDate()).isNull();
        }

        @Test
        @DisplayName("does not query repositories when every project and role id is null")
        void shouldSkipLoadWhenIdentifiersAreNull() {
            ProjectAssignment assignment = assignment(ASSIGNMENT_ID, null, LocalDate.of(2024, 1, 1), null);
            assignment.setProjectRoleId(null);
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(assignment));

            assertThatThrownBy(() -> service.getProjectHistory(EMPLOYEE_ID))
                    .isInstanceOf(NullPointerException.class);

            verify(projectRepository, never()).findAllById(any());
            verify(projectRoleRepository, never()).findAllById(any());
        }
    }

    @Nested
    @DisplayName("getTaskStatistics")
    class GetTaskStatistics {

        @Test
        @DisplayName("uses the epoch and today when the period is omitted")
        void shouldDefaultThePeriod() {
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());

            TaskStatisticsResponseDto response = service.getTaskStatistics(EMPLOYEE_ID, null, null);

            assertThat(response.periodStart()).isEqualTo(LocalDate.of(1970, 1, 1));
            assertThat(response.periodEnd()).isEqualTo(LocalDate.now());
            assertThat(response.completedTasks()).isZero();
            assertThat(response.projectsParticipated()).isZero();
            assertThat(response.completedByType()).isEmpty();
            assertThat(response.completedByComplexity()).isEmpty();
            verify(projectTaskRepository, never()).findCompletedByAssignmentIds(any(), any(), any());
        }

        @Test
        @DisplayName("swaps the period when the end is before the start")
        void shouldSwapInvertedPeriod() {
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of());

            TaskStatisticsResponseDto response = service.getTaskStatistics(
                    EMPLOYEE_ID,
                    LocalDate.of(2024, 5, 1),
                    LocalDate.of(2024, 1, 1)
            );

            assertThat(response.periodStart()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2024, 5, 1));
        }

        @Test
        @DisplayName("counts overlapping assignments and groups completed tasks")
        @SuppressWarnings("unchecked")
        void shouldAggregateCompletedTasks() {
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);
            ProjectAssignment included = assignment(ASSIGNMENT_ID, PROJECT_ID, end, null);
            ProjectAssignment sameProject = assignment(OTHER_ASSIGNMENT_ID, PROJECT_ID, null, start);
            ProjectAssignment excluded = assignment(
                    UUID.fromString("a1000000-0000-0000-0000-000000000003"),
                    OTHER_PROJECT_ID,
                    end.plusDays(1),
                    null
            );
            ProjectAssignment endedBefore = assignment(
                    UUID.fromString("a1000000-0000-0000-0000-000000000004"),
                    OTHER_PROJECT_ID,
                    start.minusDays(10),
                    start.minusDays(1)
            );

            ProjectTask story = completedTask(TYPE_ID, TaskComplexity.HIGH);
            ProjectTask unknown = completedTask(null, null);
            unknown.setId(UUID.fromString("20000000-0000-0000-0000-000000000002"));
            ProjectTask anotherStory = completedTask(TYPE_ID, TaskComplexity.HIGH);
            anotherStory.setId(UUID.fromString("20000000-0000-0000-0000-000000000003"));

            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID))
                    .thenReturn(List.of(included, sameProject, excluded, endedBefore));
            when(projectTaskRepository.findCompletedByAssignmentIds(any(), any(), any()))
                    .thenReturn(List.of(story, unknown, anotherStory));
            when(taskTypeRepository.findAllById(any())).thenReturn(List.of(
                    TaskType.builder().id(TYPE_ID).code("STORY").build()
            ));

            TaskStatisticsResponseDto response = service.getTaskStatistics(EMPLOYEE_ID, start, end);

            assertThat(response.completedTasks()).isEqualTo(3);
            assertThat(response.completedByType()).containsEntry("STORY", 2L).containsEntry("UNKNOWN", 1L);
            assertThat(response.completedByComplexity())
                    .containsEntry("HIGH", 2L)
                    .containsEntry("UNSPECIFIED", 1L);
            assertThat(response.projectsParticipated()).isEqualTo(1);

            ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<OffsetDateTime> from = ArgumentCaptor.forClass(OffsetDateTime.class);
            ArgumentCaptor<OffsetDateTime> to = ArgumentCaptor.forClass(OffsetDateTime.class);
            verify(projectTaskRepository).findCompletedByAssignmentIds(ids.capture(), from.capture(), to.capture());
            assertThat(ids.getValue()).containsExactlyInAnyOrder(ASSIGNMENT_ID, OTHER_ASSIGNMENT_ID);
            assertThat(from.getValue()).isEqualTo(start.atStartOfDay().atOffset(ZoneOffset.UTC));
            assertThat(to.getValue()).isEqualTo(end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("labels a task as unknown when its type cannot be resolved")
        void shouldUseUnknownTypeWhenLookupIsEmpty() {
            LocalDate start = LocalDate.of(2024, 2, 1);
            LocalDate end = LocalDate.of(2024, 2, 28);
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, start, end)
            ));
            when(projectTaskRepository.findCompletedByAssignmentIds(any(), any(), any()))
                    .thenReturn(List.of(completedTask(TYPE_ID, TaskComplexity.LOW)));
            when(taskTypeRepository.findAllById(Set.of(TYPE_ID))).thenReturn(List.of());

            TaskStatisticsResponseDto response = service.getTaskStatistics(EMPLOYEE_ID, start, end);

            assertThat(response.completedByType()).containsEntry("UNKNOWN", 1L);
            assertThat(response.completedByComplexity()).containsEntry("LOW", 1L);
            assertThat(response.projectsParticipated()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getMemberEligibility")
    class GetMemberEligibility {

        @Test
        @DisplayName("reports a missing project")
        void shouldReportMissingProject() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            MemberEligibilityResponseDto response = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);

            assertThat(response.eligible()).isFalse();
            assertThat(response.reason()).isEqualTo("PROJECT_NOT_FOUND");
            assertThat(response.projectActive()).isFalse();
            assertThat(response.assignmentActive()).isFalse();
            verifyNoInteractions(projectAssignmentRepository);
        }

        @Test
        @DisplayName("reports a missing assignment and still exposes the project status")
        void shouldReportMissingAssignment() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.PLANNED)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, OTHER_PROJECT_ID, LocalDate.now(), null)
            ));

            MemberEligibilityResponseDto response = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);

            assertThat(response.reason()).isEqualTo("NO_PROJECT_ASSIGNMENT");
            assertThat(response.projectActive()).isFalse();
            assertThat(response.assignmentId()).isNull();
        }

        @Test
        @DisplayName("accepts an assignment that covers today")
        void shouldAcceptActiveAssignment() {
            LocalDate today = LocalDate.now();
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.ACTIVE)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, today, today)
            ));

            MemberEligibilityResponseDto response = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);

            assertThat(response.eligible()).isTrue();
            assertThat(response.reason()).isNull();
            assertThat(response.projectActive()).isTrue();
            assertThat(response.assignmentActive()).isTrue();
            assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
            assertThat(response.assignmentStartDate()).isEqualTo(today);
            assertThat(response.assignmentEndDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("treats a null start and a null end as an open assignment")
        void shouldAcceptOpenEndedAssignment() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.ACTIVE)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, null, null)
            ));

            assertThat(service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID).eligible()).isTrue();
        }

        @Test
        @DisplayName("prefers an inactive project over an inactive assignment")
        void shouldPreferInactiveProject() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.COMPLETED)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
            ));

            MemberEligibilityResponseDto response = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);

            assertThat(response.eligible()).isFalse();
            assertThat(response.reason()).isEqualTo("PROJECT_NOT_ACTIVE");
            assertThat(response.projectActive()).isFalse();
            assertThat(response.assignmentActive()).isFalse();
            assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
        }

        @Test
        @DisplayName("reports an assignment that has not started or has already ended")
        void shouldReportInactiveAssignment() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.ACTIVE)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, LocalDate.now().plusDays(1), null)
            ));

            MemberEligibilityResponseDto future = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);
            assertThat(future.reason()).isEqualTo("ASSIGNMENT_NOT_ACTIVE");
            assertThat(future.assignmentActive()).isFalse();

            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(
                    assignment(OTHER_ASSIGNMENT_ID, PROJECT_ID, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
            ));

            MemberEligibilityResponseDto ended = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);
            assertThat(ended.reason()).isEqualTo("ASSIGNMENT_NOT_ACTIVE");
            assertThat(ended.assignmentId()).isEqualTo(OTHER_ASSIGNMENT_ID);
        }

        @Test
        @DisplayName("selects the assignment that is active today even when a newer one has ended")
        void shouldPreferTheActiveAssignment() {
            LocalDate today = LocalDate.now();
            ProjectAssignment endedRecently = assignment(
                    OTHER_ASSIGNMENT_ID, PROJECT_ID, today.minusDays(2), today.minusDays(1)
            );
            ProjectAssignment stillActive = assignment(
                    ASSIGNMENT_ID, PROJECT_ID, today.minusYears(1), null
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.ACTIVE)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID))
                    .thenReturn(List.of(stillActive, endedRecently));

            assertThat(service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID).assignmentId()).isEqualTo(ASSIGNMENT_ID);
        }

        @Test
        @DisplayName("falls back to the newest assignment when none is active")
        void shouldFallBackToTheNewestAssignment() {
            LocalDate today = LocalDate.now();
            ProjectAssignment older = assignment(
                    ASSIGNMENT_ID, PROJECT_ID, today.minusYears(2), today.minusYears(1)
            );
            ProjectAssignment newer = assignment(
                    OTHER_ASSIGNMENT_ID, PROJECT_ID, today.minusDays(20), today.minusDays(10)
            );
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project(ProjectStatus.ACTIVE)));
            when(projectAssignmentRepository.findAllByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(older, newer));

            MemberEligibilityResponseDto response = service.getMemberEligibility(PROJECT_ID, EMPLOYEE_ID);

            assertThat(response.assignmentId()).isEqualTo(OTHER_ASSIGNMENT_ID);
            assertThat(response.reason()).isEqualTo("ASSIGNMENT_NOT_ACTIVE");
        }
    }

    @Nested
    @DisplayName("getTaskCompletionEvidence")
    class GetTaskCompletionEvidence {

        @Test
        @DisplayName("reports a missing task")
        void shouldReportMissingTask() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            TaskCompletionEvidenceResponseDto response = service.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID);

            assertThat(response.taskCompleted()).isFalse();
            assertThat(response.employeeParticipated()).isFalse();
            assertThat(response.completedAt()).isNull();
            verifyNoInteractions(taskAssigneeRepository, projectAssignmentRepository);
        }

        @Test
        @DisplayName("reports completion and participation")
        void shouldReportCompletionAndParticipation() {
            OffsetDateTime completedAt = OffsetDateTime.parse("2024-07-01T12:00:00Z");
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(PROJECT_ID, null, completedAt)));
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of(
                    TaskAssignee.builder().taskId(TASK_ID).assignmentId(ASSIGNMENT_ID).build(),
                    TaskAssignee.builder().taskId(TASK_ID).assignmentId(OTHER_ASSIGNMENT_ID).build()
            ));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    assignment(ASSIGNMENT_ID, PROJECT_ID, LocalDate.of(2024, 1, 1), null),
                    assignmentFor(OTHER_ASSIGNMENT_ID, OTHER_EMPLOYEE_ID)
            ));

            TaskCompletionEvidenceResponseDto response = service.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID);

            assertThat(response.taskCompleted()).isTrue();
            assertThat(response.employeeParticipated()).isTrue();
            assertThat(response.completedAt()).isEqualTo(completedAt);
        }

        @Test
        @DisplayName("reports an open task with no assignees")
        void shouldReportOpenTaskWithoutAssignees() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(PROJECT_ID, null, null)));
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            TaskCompletionEvidenceResponseDto response = service.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID);

            assertThat(response.taskCompleted()).isFalse();
            assertThat(response.employeeParticipated()).isFalse();
            verify(projectAssignmentRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("reports that the employee did not participate")
        void shouldReportMissingParticipation() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(task(PROJECT_ID, null, null)));
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of(
                    TaskAssignee.builder().taskId(TASK_ID).assignmentId(ASSIGNMENT_ID).build()
            ));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    assignmentFor(ASSIGNMENT_ID, OTHER_EMPLOYEE_ID)
            ));

            assertThat(service.getTaskCompletionEvidence(TASK_ID, EMPLOYEE_ID).employeeParticipated()).isFalse();
        }
    }

    private WorkEligibilityResponseDto eligibility(UUID taskId) {
        return service.getWorkEligibility(PROJECT_ID, EMPLOYEE_ID, taskId, WORK_DATE);
    }

    private void stubProject(ProjectStatus status, LocalDate startDate, LocalDate endDate) {
        Project project = project(status);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    private Project project(ProjectStatus status) {
        return Project.builder()
                .id(PROJECT_ID)
                .code("PRJ")
                .name("Project")
                .status(status)
                .startDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    private ProjectAssignment assignment(UUID id, UUID projectId, LocalDate startDate, LocalDate endDate) {
        return ProjectAssignment.builder()
                .id(id)
                .projectId(projectId)
                .employeeId(EMPLOYEE_ID)
                .projectRoleId(ROLE_ID)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private ProjectAssignment assignmentFor(UUID id, UUID employeeId) {
        ProjectAssignment assignment = assignment(id, PROJECT_ID, LocalDate.of(2024, 1, 1), null);
        assignment.setEmployeeId(employeeId);
        return assignment;
    }

    private ProjectTask task(UUID projectId, OffsetDateTime archivedAt, OffsetDateTime completedAt) {
        return ProjectTask.builder()
                .id(TASK_ID)
                .projectId(projectId)
                .title("Task")
                .archivedAt(archivedAt)
                .completedAt(completedAt)
                .build();
    }

    private ProjectTask completedTask(UUID taskTypeId, TaskComplexity complexity) {
        return ProjectTask.builder()
                .id(TASK_ID)
                .projectId(PROJECT_ID)
                .taskTypeId(taskTypeId)
                .complexity(complexity)
                .title("Done")
                .completedAt(OffsetDateTime.parse("2024-01-15T00:00:00Z"))
                .build();
    }
}
