package com.zodiakk.projectservice.security.access;

import com.zodiakk.projectservice.board.entity.BoardColumn;
import com.zodiakk.projectservice.board.entity.ProjectBoard;
import com.zodiakk.projectservice.board.repository.BoardColumnRepository;
import com.zodiakk.projectservice.board.repository.ProjectBoardRepository;
import com.zodiakk.projectservice.common.constants.SecurityConstants;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceNotFoundException;
import com.zodiakk.projectservice.integration.exception.EmployeeServiceUnavailableException;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.entity.ProjectLink;
import com.zodiakk.projectservice.project.entity.ProjectMilestone;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectLinkRepository;
import com.zodiakk.projectservice.project.repository.ProjectMilestoneRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.security.principal.CurrentUser;
import com.zodiakk.projectservice.task.entity.ProjectTask;
import com.zodiakk.projectservice.task.entity.TaskAssignee;
import com.zodiakk.projectservice.task.entity.TaskRelation;
import com.zodiakk.projectservice.task.entity.TaskSkill;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskAssigneeRepository;
import com.zodiakk.projectservice.task.repository.TaskRelationRepository;
import com.zodiakk.projectservice.task.repository.TaskSkillRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID PROJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID TASK_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID OTHER_TASK_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final UUID ASSIGNMENT_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID OTHER_ASSIGNMENT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final UUID MILESTONE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final UUID LINK_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID BOARD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID COLUMN_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID RELATION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final UUID TASK_SKILL_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private static final String ADMIN_ROLE = SecurityConstants.ADMIN_AUTHORITY;

    private static final String HR_ROLE = SecurityConstants.HR_AUTHORITY;

    private static final String MANAGER_ROLE = SecurityConstants.MANAGER_AUTHORITY;

    private static final String EMPLOYEE_ROLE = SecurityConstants.EMPLOYEE_AUTHORITY;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private TaskAssigneeRepository taskAssigneeRepository;

    @Mock
    private ProjectMilestoneRepository projectMilestoneRepository;

    @Mock
    private ProjectLinkRepository projectLinkRepository;

    @Mock
    private ProjectBoardRepository projectBoardRepository;

    @Mock
    private BoardColumnRepository boardColumnRepository;

    @Mock
    private TaskRelationRepository taskRelationRepository;

    @Mock
    private TaskSkillRepository taskSkillRepository;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @InjectMocks
    private ProjectAccessService projectAccessService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdmin {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ROLE_ADMIN, true",
                "ROLE_HR, false",
                "ROLE_MANAGER, false",
                "ROLE_EMPLOYEE, false"
        })
        @DisplayName("is true only for ADMIN")
        void shouldMatchAdminRole(String role, boolean expected) {
            authenticate(role);

            assertThat(projectAccessService.isAdmin()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("isPrivileged")
    class IsPrivileged {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ROLE_ADMIN, true",
                "ROLE_HR, true",
                "ROLE_MANAGER, false",
                "ROLE_EMPLOYEE, false"
        })
        @DisplayName("is true for ADMIN and HR")
        void shouldMatchPrivilegedRoles(String role, boolean expected) {
            authenticate(role);

            assertThat(projectAccessService.isPrivileged()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("canViewAllProjects")
    class CanViewAllProjects {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ROLE_ADMIN, true",
                "ROLE_HR, true",
                "ROLE_MANAGER, true",
                "ROLE_EMPLOYEE, false"
        })
        @DisplayName("is true for ADMIN, HR, and MANAGER")
        void shouldMatchViewAllRoles(String role, boolean expected) {
            authenticate(role);

            assertThat(projectAccessService.canViewAllProjects()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("canCreateProject")
    class CanCreateProject {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ROLE_ADMIN, true",
                "ROLE_MANAGER, true",
                "ROLE_HR, false",
                "ROLE_EMPLOYEE, false"
        })
        @DisplayName("is true for ADMIN and MANAGER")
        void shouldMatchCreateRoles(String role, boolean expected) {
            authenticate(role);

            assertThat(projectAccessService.canCreateProject()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("canManageCatalogs")
    class CanManageCatalogs {

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "ROLE_ADMIN, true",
                "ROLE_HR, false",
                "ROLE_MANAGER, false",
                "ROLE_EMPLOYEE, false"
        })
        @DisplayName("is true only for ADMIN")
        void shouldMatchCatalogRoles(String role, boolean expected) {
            authenticate(role);

            assertThat(projectAccessService.canManageCatalogs()).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("canViewProject")
    class CanViewProject {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER"})
        @DisplayName("is true for roles that can view every project")
        void shouldBeTrueForViewAllRoles(String role) {
            authenticate(role);

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isTrue();
            verifyNoInteractions(projectRepository, projectAssignmentRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isTrue();
            verify(projectAssignmentRepository, never()).existsByProjectIdAndEmployeeId(any(), any());
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, EMPLOYEE_ID);

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isTrue();
            verify(projectAssignmentRepository, never()).existsByProjectIdAndEmployeeId(any(), any());
        }

        @Test
        @DisplayName("is true for an assignee")
        void shouldBeTrueForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);
            when(projectAssignmentRepository.existsByProjectIdAndEmployeeId(PROJECT_ID, EMPLOYEE_ID)).thenReturn(true);

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an employee who is not on the project")
        void shouldBeFalseForOutsider() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, null);

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isFalse();
            verify(projectAssignmentRepository).existsByProjectIdAndEmployeeId(PROJECT_ID, EMPLOYEE_ID);
        }

        @Test
        @DisplayName("is false when the project does not exist")
        void shouldBeFalseWhenProjectIsMissing() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isFalse();
            verify(projectAssignmentRepository, never()).existsByProjectIdAndEmployeeId(any(), any());
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isFalse();
            verifyNoInteractions(projectRepository, projectAssignmentRepository);
        }

        @Test
        @DisplayName("is false when the employee directory has no record")
        void shouldBeFalseWhenEmployeeReferenceIsMissing() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId())
                    .thenThrow(new EmployeeReferenceNotFoundException(USER_ID));

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }

        @Test
        @DisplayName("is false when the employee directory is unavailable")
        void shouldBeFalseWhenEmployeeDirectoryIsUnavailable() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId())
                    .thenThrow(new EmployeeServiceUnavailableException(new IllegalStateException("down")));

            assertThat(projectAccessService.canViewProject(PROJECT_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canEditProject")
    class CanEditProject {

        @Test
        @DisplayName("is true for ADMIN without loading the project")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService, projectAssignmentRepository);
        }

        @Test
        @DisplayName("is false for HR who does not manage the project")
        void shouldBeFalseForHr() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for HR who is the project manager")
        void shouldBeTrueForHrProjectManager() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for a MANAGER who does not manage the project")
        void shouldBeFalseForManagerRole() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, OTHER_EMPLOYEE_ID);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, OTHER_EMPLOYEE_ID);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, EMPLOYEE_ID);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an assignee who is not a manager")
        void shouldBeFalseForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isFalse();
            verify(projectAssignmentRepository, never()).existsByProjectIdAndEmployeeId(any(), any());
        }

        @Test
        @DisplayName("is false when the project does not exist")
        void shouldBeFalseWhenProjectIsMissing() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditProject(PROJECT_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canManageProjectMembers")
    class CanManageProjectMembers {

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for HR without project membership")
        void shouldBeTrueForHr() {
            authenticate(HR_ROLE);

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is false for a MANAGER who does not manage the project")
        void shouldBeFalseForManagerRole() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for a MANAGER who is the project manager")
        void shouldBeTrueForManagerWhoLeadsTheProject() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, EMPLOYEE_ID);

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an assignee who is not a manager")
        void shouldBeFalseForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, null);

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the project does not exist")
        void shouldBeFalseWhenProjectIsMissing() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canManageProjectMembers(PROJECT_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canViewTask")
    class CanViewTask {

        @Test
        @DisplayName("is false when the task does not exist")
        void shouldBeFalseWhenTaskIsMissing() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canViewTask(TASK_ID)).isFalse();
            verifyNoInteractions(projectRepository, taskAssigneeRepository, employeeDirectoryService);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER"})
        @DisplayName("is true for roles that can view every project")
        void shouldBeTrueForViewAllRoles(String role) {
            authenticate(role);
            stubTask();

            assertThat(projectAccessService.canViewTask(TASK_ID)).isTrue();
            verifyNoInteractions(employeeDirectoryService, projectRepository, taskAssigneeRepository);
        }

        @Test
        @DisplayName("is true for a project participant who is not an assignee")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(EMPLOYEE_ID, null);

            assertThat(projectAccessService.canViewTask(TASK_ID)).isTrue();
            verify(taskAssigneeRepository, never()).findAllByTaskIdAndUnassignedAtIsNull(any());
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(null, EMPLOYEE_ID);

            assertThat(projectAccessService.canViewTask(TASK_ID)).isTrue();
            verify(taskAssigneeRepository, never()).findAllByTaskIdAndUnassignedAtIsNull(any());
        }

        @Test
        @DisplayName("is true for an assignee who cannot otherwise view the project")
        void shouldBeTrueForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, null);
            when(projectAssignmentRepository.existsByProjectIdAndEmployeeId(PROJECT_ID, EMPLOYEE_ID)).thenReturn(false);
            stubMatchingAssignees();

            assertThat(projectAccessService.canViewTask(TASK_ID)).isTrue();
        }

        @Test
        @DisplayName("is false when the task has no active assignees")
        void shouldBeFalseWhenAssigneesAreEmpty() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(null, null);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            assertThat(projectAccessService.canViewTask(TASK_ID)).isFalse();
            verify(projectAssignmentRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("is false when assignee rows do not resolve to assignments")
        void shouldBeFalseWhenAssignmentsAreMissing() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(null, null);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of(assignee(ASSIGNMENT_ID)));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of());

            assertThat(projectAccessService.canViewTask(TASK_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the assignee belongs to another employee")
        void shouldBeFalseForAnotherEmployeesAssignment() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(null, null);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of(assignee(ASSIGNMENT_ID)));
            when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                    ProjectAssignment.builder().id(ASSIGNMENT_ID).employeeId(OTHER_EMPLOYEE_ID).build()
            ));

            assertThat(projectAccessService.canViewTask(TASK_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            stubTask();
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canViewTask(TASK_ID)).isFalse();
            verifyNoInteractions(projectRepository, taskAssigneeRepository);
        }
    }

    @Nested
    @DisplayName("canEditTask")
    class CanEditTask {

        @Test
        @DisplayName("is false when the task does not exist")
        void shouldBeFalseWhenTaskIsMissing() {
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for ADMIN without a project or employee lookup")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubTask();

            assertThat(projectAccessService.canEditTask(TASK_ID)).isTrue();
            verifyNoInteractions(employeeDirectoryService, projectRepository, taskAssigneeRepository);
        }

        @Test
        @DisplayName("is false for HR who is neither a manager nor an assignee")
        void shouldBeFalseForHr() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, null);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for HR who is assigned to the task")
        void shouldBeTrueForHrAssignee() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubMatchingAssignees();

            assertThat(projectAccessService.canEditTask(TASK_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for a MANAGER who is neither a manager of the project nor an assignee")
        void shouldBeFalseForManagerRole() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, OTHER_EMPLOYEE_ID);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(EMPLOYEE_ID, null);

            assertThat(projectAccessService.canEditTask(TASK_ID)).isTrue();
            verify(taskAssigneeRepository, never()).findAllByTaskIdAndUnassignedAtIsNull(any());
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(null, EMPLOYEE_ID);

            assertThat(projectAccessService.canEditTask(TASK_ID)).isTrue();
            verify(taskAssigneeRepository, never()).findAllByTaskIdAndUnassignedAtIsNull(any());
        }

        @Test
        @DisplayName("is true for an assignee")
        void shouldBeTrueForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubMatchingAssignees();

            assertThat(projectAccessService.canEditTask(TASK_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an employee who is not a manager or assignee")
        void shouldBeFalseForOutsider() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTask();
            stubProject(null, null);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            stubTask();
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
            verifyNoInteractions(projectRepository, taskAssigneeRepository);
        }

        @Test
        @DisplayName("is false when the employee directory is unavailable")
        void shouldBeFalseWhenEmployeeDirectoryIsUnavailable() {
            authenticate(EMPLOYEE_ROLE);
            stubTask();
            when(employeeDirectoryService.findCurrentEmployeeId())
                    .thenThrow(new EmployeeServiceUnavailableException(new IllegalStateException("down")));

            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
            verifyNoInteractions(projectRepository, taskAssigneeRepository);
        }
    }

    @Nested
    @DisplayName("canEditMilestone")
    class CanEditMilestone {

        @Test
        @DisplayName("is false when the milestone does not exist")
        void shouldBeFalseWhenMissing() {
            when(projectMilestoneRepository.findById(MILESTONE_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isFalse();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubMilestone();

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is false for HR who does not manage the project")
        void shouldBeFalseForHr() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubMilestone();

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);
            stubMilestone();

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, EMPLOYEE_ID);
            stubMilestone();

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an assignee who is not a manager")
        void shouldBeFalseForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubMilestone();

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());
            stubMilestone();

            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canEditLink")
    class CanEditLink {

        @Test
        @DisplayName("is false when the link does not exist")
        void shouldBeFalseWhenMissing() {
            when(projectLinkRepository.findById(LINK_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditLink(LINK_ID)).isFalse();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubLink();

            assertThat(projectAccessService.canEditLink(LINK_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is false for HR who does not manage the project")
        void shouldBeFalseForHr() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, OTHER_EMPLOYEE_ID);
            stubLink();

            assertThat(projectAccessService.canEditLink(LINK_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);
            stubLink();

            assertThat(projectAccessService.canEditLink(LINK_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, EMPLOYEE_ID);
            stubLink();

            assertThat(projectAccessService.canEditLink(LINK_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an assignee who is not a manager")
        void shouldBeFalseForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubLink();

            assertThat(projectAccessService.canEditLink(LINK_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());
            stubLink();

            assertThat(projectAccessService.canEditLink(LINK_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canEditBoard")
    class CanEditBoard {

        @Test
        @DisplayName("is false when the board does not exist")
        void shouldBeFalseWhenMissing() {
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isFalse();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubBoard();

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is false for a MANAGER who does not manage the project")
        void shouldBeFalseForManagerRole() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubBoard();

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);
            stubBoard();

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, EMPLOYEE_ID);
            stubBoard();

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an assignee who is not a manager")
        void shouldBeFalseForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, OTHER_EMPLOYEE_ID);
            stubBoard();

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(HR_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());
            stubBoard();

            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canEditBoardColumn")
    class CanEditBoardColumn {

        @Test
        @DisplayName("is false when the column does not exist")
        void shouldBeFalseWhenColumnIsMissing() {
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isFalse();
            verifyNoInteractions(projectBoardRepository, projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is false when the column's board does not exist")
        void shouldBeFalseWhenBoardIsMissing() {
            authenticate(ADMIN_ROLE);
            when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column()));
            when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isFalse();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubColumnAndBoard();

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isTrue();
            verifyNoInteractions(projectRepository, employeeDirectoryService);
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(OTHER_EMPLOYEE_ID, EMPLOYEE_ID);
            stubColumnAndBoard();

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the project manager")
        void shouldBeTrueForProjectManager() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(EMPLOYEE_ID, null);
            stubColumnAndBoard();

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an assignee who is not a manager")
        void shouldBeFalseForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubProject(null, null);
            stubColumnAndBoard();

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());
            stubColumnAndBoard();

            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isFalse();
            verifyNoInteractions(projectRepository);
        }
    }

    @Nested
    @DisplayName("canEditTaskRelation")
    class CanEditTaskRelation {

        @Test
        @DisplayName("is false when the relation does not exist")
        void shouldBeFalseWhenMissing() {
            when(taskRelationRepository.findById(RELATION_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isFalse();
            verifyNoInteractions(projectTaskRepository);
        }

        @Test
        @DisplayName("is false when the source task does not exist")
        void shouldBeFalseWhenSourceTaskIsMissing() {
            authenticate(ADMIN_ROLE);
            stubRelation();
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isFalse();
            verify(projectTaskRepository, never()).findById(OTHER_TASK_ID);
        }

        @Test
        @DisplayName("is true for ADMIN of the source task")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubRelation();
            stubTask();

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isTrue();
            verify(projectTaskRepository).findById(TASK_ID);
            verify(projectTaskRepository, never()).findById(OTHER_TASK_ID);
        }

        @Test
        @DisplayName("is true for an assignee of the source task")
        void shouldBeTrueForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubRelation();
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubMatchingAssignees();

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for the project manager of the source task")
        void shouldBeTrueForProjectManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubRelation();
            stubTask();
            stubProject(EMPLOYEE_ID, null);

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an employee who cannot edit the source task")
        void shouldBeFalseForOutsider() {
            authenticate(HR_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubRelation();
            stubTask();
            stubProject(null, OTHER_EMPLOYEE_ID);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(MANAGER_ROLE);
            stubRelation();
            stubTask();
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("canEditTaskSkill")
    class CanEditTaskSkill {

        @Test
        @DisplayName("is false when the task skill does not exist")
        void shouldBeFalseWhenMissing() {
            when(taskSkillRepository.findById(TASK_SKILL_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isFalse();
            verifyNoInteractions(projectTaskRepository);
        }

        @Test
        @DisplayName("is false when the task does not exist")
        void shouldBeFalseWhenTaskIsMissing() {
            authenticate(ADMIN_ROLE);
            stubTaskSkill();
            when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isFalse();
        }

        @Test
        @DisplayName("is true for ADMIN")
        void shouldBeTrueForAdmin() {
            authenticate(ADMIN_ROLE);
            stubTaskSkill();
            stubTask();

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isTrue();
            verify(projectTaskRepository).findById(TASK_ID);
        }

        @Test
        @DisplayName("is true for the delivery manager")
        void shouldBeTrueForDeliveryManager() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTaskSkill();
            stubTask();
            stubProject(null, EMPLOYEE_ID);

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isTrue();
        }

        @Test
        @DisplayName("is true for an assignee")
        void shouldBeTrueForAssignee() {
            authenticate(EMPLOYEE_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTaskSkill();
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, null);
            stubMatchingAssignees();

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isTrue();
        }

        @Test
        @DisplayName("is false for an employee who cannot edit the task")
        void shouldBeFalseForOutsider() {
            authenticate(MANAGER_ROLE);
            stubCurrentEmployee(EMPLOYEE_ID);
            stubTaskSkill();
            stubTask();
            stubProject(OTHER_EMPLOYEE_ID, OTHER_EMPLOYEE_ID);
            when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of());

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isFalse();
        }

        @Test
        @DisplayName("is false when the current employee cannot be resolved")
        void shouldBeFalseWithoutCurrentEmployee() {
            authenticate(EMPLOYEE_ROLE);
            stubTaskSkill();
            stubTask();
            when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.empty());

            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isFalse();
            verifyNoInteractions(projectRepository, taskAssigneeRepository);
        }
    }

    @Nested
    @DisplayName("unauthenticated")
    class Unauthenticated {

        @Test
        @DisplayName("rejects role checks when nobody is authenticated")
        void shouldRejectRoleChecks() {
            assertThatThrownBy(projectAccessService::isAdmin).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(projectAccessService::isPrivileged).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(projectAccessService::canViewAllProjects).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(projectAccessService::canCreateProject).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(projectAccessService::canManageCatalogs).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canViewProject(PROJECT_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditProject(PROJECT_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canManageProjectMembers(PROJECT_ID))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("denies a missing task, milestone, link, board, column, relation, and skill")
        void shouldDenyMissingResources() {
            assertThat(projectAccessService.canViewTask(TASK_ID)).isFalse();
            assertThat(projectAccessService.canEditTask(TASK_ID)).isFalse();
            assertThat(projectAccessService.canEditMilestone(MILESTONE_ID)).isFalse();
            assertThat(projectAccessService.canEditLink(LINK_ID)).isFalse();
            assertThat(projectAccessService.canEditBoard(BOARD_ID)).isFalse();
            assertThat(projectAccessService.canEditBoardColumn(COLUMN_ID)).isFalse();
            assertThat(projectAccessService.canEditTaskRelation(RELATION_ID)).isFalse();
            assertThat(projectAccessService.canEditTaskSkill(TASK_SKILL_ID)).isFalse();
        }

        @Test
        @DisplayName("fails when a present resource still requires a role check")
        void shouldThrowWhenRoleCheckRunsWithoutAuthentication() {
            stubTask();
            stubMilestone();
            stubLink();
            stubBoard();
            stubColumnAndBoard();
            stubRelation();
            stubTaskSkill();

            assertThatThrownBy(() -> projectAccessService.canViewTask(TASK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Current user is not authenticated");
            assertThatThrownBy(() -> projectAccessService.canEditTask(TASK_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditMilestone(MILESTONE_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditLink(LINK_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditBoard(BOARD_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditBoardColumn(COLUMN_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditTaskRelation(RELATION_ID))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> projectAccessService.canEditTaskSkill(TASK_SKILL_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private void authenticate(String role) {
        CurrentUser currentUser = CurrentUser.builder()
                .userId(USER_ID)
                .role(role)
                .email("user@company.com")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
    }

    private void stubCurrentEmployee(UUID employeeId) {
        when(employeeDirectoryService.findCurrentEmployeeId()).thenReturn(Optional.of(employeeId));
    }

    private void stubProject(UUID projectManagerId, UUID deliveryManagerId) {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(
                Project.builder()
                        .id(PROJECT_ID)
                        .projectManagerId(projectManagerId)
                        .deliveryManagerId(deliveryManagerId)
                        .build()
        ));
    }

    private void stubTask() {
        when(projectTaskRepository.findById(TASK_ID)).thenReturn(Optional.of(
                ProjectTask.builder().id(TASK_ID).projectId(PROJECT_ID).title("Task").build()
        ));
    }

    private void stubMilestone() {
        when(projectMilestoneRepository.findById(MILESTONE_ID)).thenReturn(Optional.of(
                ProjectMilestone.builder().id(MILESTONE_ID).projectId(PROJECT_ID).title("Milestone").status("OPEN").build()
        ));
    }

    private void stubLink() {
        when(projectLinkRepository.findById(LINK_ID)).thenReturn(Optional.of(
                ProjectLink.builder().id(LINK_ID).projectId(PROJECT_ID).label("Wiki").url("https://example.test").build()
        ));
    }

    private void stubBoard() {
        when(projectBoardRepository.findById(BOARD_ID)).thenReturn(Optional.of(
                ProjectBoard.builder().id(BOARD_ID).projectId(PROJECT_ID).name("Board").build()
        ));
    }

    private void stubColumnAndBoard() {
        when(boardColumnRepository.findById(COLUMN_ID)).thenReturn(Optional.of(column()));
        stubBoard();
    }

    private void stubRelation() {
        when(taskRelationRepository.findById(RELATION_ID)).thenReturn(Optional.of(
                TaskRelation.builder().id(RELATION_ID).sourceTaskId(TASK_ID).targetTaskId(OTHER_TASK_ID).build()
        ));
    }

    private void stubTaskSkill() {
        when(taskSkillRepository.findById(TASK_SKILL_ID)).thenReturn(Optional.of(
                TaskSkill.builder().id(TASK_SKILL_ID).taskId(TASK_ID).skillId(OTHER_TASK_ID).build()
        ));
    }

    private void stubMatchingAssignees() {
        when(taskAssigneeRepository.findAllByTaskIdAndUnassignedAtIsNull(TASK_ID)).thenReturn(List.of(
                assignee(OTHER_ASSIGNMENT_ID),
                assignee(ASSIGNMENT_ID)
        ));
        when(projectAssignmentRepository.findAllById(any())).thenReturn(List.of(
                ProjectAssignment.builder().id(OTHER_ASSIGNMENT_ID).employeeId(OTHER_EMPLOYEE_ID).build(),
                ProjectAssignment.builder().id(ASSIGNMENT_ID).projectId(PROJECT_ID).employeeId(EMPLOYEE_ID).build()
        ));
    }

    private static BoardColumn column() {
        return BoardColumn.builder().id(COLUMN_ID).boardId(BOARD_ID).code("TODO").name("To do").build();
    }

    private static TaskAssignee assignee(UUID assignmentId) {
        return TaskAssignee.builder().taskId(TASK_ID).assignmentId(assignmentId).build();
    }
}
