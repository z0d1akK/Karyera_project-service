package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;
import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceNotFoundException;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectResponseDto;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.exception.project.InvalidProjectDatesException;
import com.zodiakk.projectservice.project.exception.project.InvalidProjectStatusTransitionException;
import com.zodiakk.projectservice.project.exception.project.ProjectCodeAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectMapper;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.security.access.ProjectAccessService;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID PM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID DM_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID CURRENT_EMPLOYEE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final String CODE = "PRJ-1";

    private static final String NAME = "Apollo";

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    private static final long MEMBER_COUNT = 4L;

    private static final long TASK_COUNT = 9L;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("maps a page for a caller who can view every project")
        void shouldReturnMappedPageWhenCallerCanViewAllProjects() {
            ProjectFilterDto filter = ProjectFilterDto.builder()
                    .code("prj")
                    .name("apol")
                    .status(ProjectStatus.ACTIVE)
                    .projectManagerId(PM_ID)
                    .deliveryManagerId(DM_ID)
                    .startDateFrom(START)
                    .startDateTo(END)
                    .endDateFrom(START)
                    .endDateTo(END)
                    .build();
            PageRequestDto pageRequest = PageRequestDto.builder()
                    .page(1)
                    .size(5)
                    .sortBy("name")
                    .sortDirection(SortDirection.DESC)
                    .build();
            Project project = project(ProjectStatus.ACTIVE);
            project.setProjectManagerId(PM_ID);
            project.setDeliveryManagerId(DM_ID);
            EmployeeReferenceDto projectManager = employee(PM_ID, "Ada Lovelace");
            EmployeeReferenceDto deliveryManager = employee(DM_ID, "Grace Hopper");
            ProjectResponseDto response = response();
            Page<Project> page = new PageImpl<>(List.of(project), PageRequest.of(1, 5), 6);

            when(projectAccessService.canViewAllProjects()).thenReturn(true);
            when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(
                    PM_ID, projectManager,
                    DM_ID, deliveryManager
            ));
            when(projectMapper.toResponseDto(project)).thenReturn(response);
            stubCounts(PROJECT_ID);

            Page<ProjectResponseDto> result = projectService.findAll(filter, pageRequest);

            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);
            assertThat(response.getProjectManager()).isSameAs(projectManager);
            assertThat(response.getDeliveryManager()).isSameAs(deliveryManager);
            assertThat(response.getMemberCount()).isEqualTo(MEMBER_COUNT);
            assertThat(response.getTaskCount()).isEqualTo(TASK_COUNT);
            assertThat(resolvedIds()).containsExactly(PM_ID, DM_ID);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectRepository).findAll(any(Specification.class), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
            verify(employeeDirectoryService, never()).getCurrentEmployee();
        }

        @Test
        @DisplayName("limits the query to the current employee and leaves absent managers untouched")
        void shouldRestrictProjectsToTheCurrentEmployee() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectResponseDto response = response();
            EmployeeReferenceDto presetManager = employee(PM_ID, "Preset");
            EmployeeReferenceDto presetDelivery = employee(DM_ID, "Preset delivery");
            response.setProjectManager(presetManager);
            response.setDeliveryManager(presetDelivery);

            when(projectAccessService.canViewAllProjects()).thenReturn(false);
            when(employeeDirectoryService.getCurrentEmployee()).thenReturn(employee(CURRENT_EMPLOYEE_ID, "Current"));
            when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(project)));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            Page<ProjectResponseDto> result = projectService.findAll(
                    ProjectFilterDto.builder().build(),
                    PageRequestDto.builder().page(0).size(20).build()
            );

            assertThat(result.getContent()).containsExactly(response);
            assertThat(response.getProjectManager()).isSameAs(presetManager);
            assertThat(response.getDeliveryManager()).isSameAs(presetDelivery);
            assertThat(response.getMemberCount()).isZero();
            assertThat(response.getTaskCount()).isZero();
            assertThat(resolvedIds()).isEmpty();
            verify(employeeDirectoryService).getCurrentEmployee();
        }

        @Test
        @DisplayName("returns an empty page without mapping projects")
        void shouldReturnEmptyPage() {
            when(projectAccessService.canViewAllProjects()).thenReturn(true);
            when(projectRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());

            Page<ProjectResponseDto> result = projectService.findAll(
                    ProjectFilterDto.builder().name("missing").build(),
                    PageRequestDto.builder().page(0).size(20).build()
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(projectMapper, never()).toResponseDto(any());
            verifyNoInteractions(projectAssignmentRepository, projectTaskRepository);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns both managers, the member count, and the task count")
        void shouldReturnProjectWithBothManagers() {
            Project project = managedProject(PM_ID, DM_ID);
            EmployeeReferenceDto projectManager = employee(PM_ID, "Ada Lovelace");
            EmployeeReferenceDto deliveryManager = employee(DM_ID, "Grace Hopper");
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(
                    PM_ID, projectManager,
                    DM_ID, deliveryManager
            ));
            when(projectMapper.toResponseDto(project)).thenReturn(response);
            stubCounts(PROJECT_ID);

            ProjectResponseDto result = projectService.getById(PROJECT_ID);

            assertThat(result).isSameAs(response);
            assertThat(result.getProjectManager()).isSameAs(projectManager);
            assertThat(result.getDeliveryManager()).isSameAs(deliveryManager);
            assertThat(result.getMemberCount()).isEqualTo(MEMBER_COUNT);
            assertThat(result.getTaskCount()).isEqualTo(TASK_COUNT);
            assertThat(resolvedIds()).containsExactly(PM_ID, DM_ID);
        }

        @Test
        @DisplayName("resolves only the project manager")
        void shouldResolveOnlyTheProjectManager() {
            Project project = managedProject(PM_ID, null);
            EmployeeReferenceDto projectManager = employee(PM_ID, "Ada Lovelace");
            EmployeeReferenceDto presetDelivery = employee(DM_ID, "Preset delivery");
            ProjectResponseDto response = response();
            response.setDeliveryManager(presetDelivery);

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(PM_ID, projectManager));
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            ProjectResponseDto result = projectService.getById(PROJECT_ID);

            assertThat(result.getProjectManager()).isSameAs(projectManager);
            assertThat(result.getDeliveryManager()).isSameAs(presetDelivery);
            assertThat(resolvedIds()).containsExactly(PM_ID);
        }

        @Test
        @DisplayName("resolves only the delivery manager")
        void shouldResolveOnlyTheDeliveryManager() {
            Project project = managedProject(null, DM_ID);
            EmployeeReferenceDto deliveryManager = employee(DM_ID, "Grace Hopper");
            EmployeeReferenceDto presetManager = employee(PM_ID, "Preset");
            ProjectResponseDto response = response();
            response.setProjectManager(presetManager);

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(DM_ID, deliveryManager));
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            ProjectResponseDto result = projectService.getById(PROJECT_ID);

            assertThat(result.getProjectManager()).isSameAs(presetManager);
            assertThat(result.getDeliveryManager()).isSameAs(deliveryManager);
            assertThat(resolvedIds()).containsExactly(DM_ID);
        }

        @Test
        @DisplayName("resolves one employee when the same person holds both manager roles")
        void shouldResolveSharedManagerOnce() {
            Project project = managedProject(PM_ID, PM_ID);
            EmployeeReferenceDto manager = employee(PM_ID, "Ada Lovelace");
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(PM_ID, manager));
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            ProjectResponseDto result = projectService.getById(PROJECT_ID);

            assertThat(result.getProjectManager()).isSameAs(manager);
            assertThat(result.getDeliveryManager()).isSameAs(manager);
            assertThat(resolvedIds()).containsExactly(PM_ID);
        }

        @Test
        @DisplayName("clears a manager reference when the directory has no match")
        void shouldClearManagerWhenDirectoryMissesTheId() {
            Project project = managedProject(PM_ID, null);
            EmployeeReferenceDto presetManager = employee(PM_ID, "Preset");
            EmployeeReferenceDto presetDelivery = employee(DM_ID, "Preset delivery");
            ProjectResponseDto response = response();
            response.setProjectManager(presetManager);
            response.setDeliveryManager(presetDelivery);

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            ProjectResponseDto result = projectService.getById(PROJECT_ID);

            assertThat(result.getProjectManager()).isNull();
            assertThat(result.getDeliveryManager()).isSameAs(presetDelivery);
        }

        @Test
        @DisplayName("throws when the project does not exist")
        void shouldThrowWhenProjectIsMissing() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getById(PROJECT_ID))
                    .isInstanceOf(ProjectNotFoundException.class)
                    .hasMessageContaining(PROJECT_ID.toString());

            verifyNoInteractions(
                    projectMapper,
                    employeeDirectoryService,
                    projectAssignmentRepository,
                    projectTaskRepository
            );
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates a planned project and publishes a created event")
        void shouldCreatePlannedProject() {
            ProjectCreateRequestDto request = createRequest(CODE, START, END, PM_ID, DM_ID);
            Project project = project(ProjectStatus.ACTIVE);
            project.setProjectManagerId(PM_ID);
            project.setDeliveryManagerId(DM_ID);
            EmployeeReferenceDto projectManager = employee(PM_ID, "Ada Lovelace");
            EmployeeReferenceDto deliveryManager = employee(DM_ID, "Grace Hopper");
            ProjectResponseDto response = response();

            when(projectRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(projectMapper.toEntity(request)).thenReturn(project);
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(
                    PM_ID, projectManager,
                    DM_ID, deliveryManager
            ));
            when(projectMapper.toResponseDto(project)).thenReturn(response);
            stubCounts(PROJECT_ID);

            ProjectResponseDto result = projectService.create(request);

            assertThat(result).isSameAs(response);
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
            assertThat(result.getProjectManager()).isSameAs(projectManager);
            assertThat(result.getDeliveryManager()).isSameAs(deliveryManager);
            assertThat(result.getMemberCount()).isEqualTo(MEMBER_COUNT);
            assertThat(result.getTaskCount()).isEqualTo(TASK_COUNT);
            assertThat(requiredIds()).containsExactly(PM_ID, DM_ID);
            assertThat(projectEvent(EventConstants.PROJECT_CREATED))
                    .containsEntry("projectId", PROJECT_ID)
                    .containsEntry("code", CODE)
                    .containsEntry("name", NAME)
                    .containsEntry("status", ProjectStatus.PLANNED)
                    .containsEntry("startDate", START)
                    .containsEntry("endDate", END)
                    .containsEntry("projectManagerId", PM_ID)
                    .containsEntry("deliveryManagerId", DM_ID);
        }

        @Test
        @DisplayName("creates a project without a code, managers, or end date")
        void shouldCreateWithoutCodeManagersOrEndDate() {
            ProjectCreateRequestDto request = createRequest(null, START, null, null, null);
            Project project = Project.builder()
                    .id(PROJECT_ID)
                    .name(NAME)
                    .startDate(START)
                    .status(ProjectStatus.CANCELLED)
                    .build();
            ProjectResponseDto response = response();

            when(projectMapper.toEntity(request)).thenReturn(project);
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.create(request)).isSameAs(response);
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);

            verify(projectRepository, never()).existsByCodeIgnoreCase(any());
            verify(employeeDirectoryService, never()).requireAll(any());
            assertThat(projectEvent(EventConstants.PROJECT_CREATED))
                    .containsEntry("status", ProjectStatus.PLANNED)
                    .containsEntry("endDate", null)
                    .containsEntry("projectManagerId", null)
                    .containsEntry("deliveryManagerId", null);
        }

        @Test
        @DisplayName("creates a project with only a delivery manager when the start date is missing")
        void shouldCreateWhenStartDateIsMissing() {
            ProjectCreateRequestDto request = createRequest(CODE, null, END, null, DM_ID);
            Project project = Project.builder()
                    .id(PROJECT_ID)
                    .code(CODE)
                    .name(NAME)
                    .endDate(END)
                    .deliveryManagerId(DM_ID)
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(projectMapper.toEntity(request)).thenReturn(project);
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(DM_ID, employee(DM_ID, "Grace")));
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.create(request)).isSameAs(response);
            assertThat(requiredIds()).containsExactly(DM_ID);
        }

        @Test
        @DisplayName("creates a project with only a project manager")
        void shouldCreateWithOnlyTheProjectManager() {
            ProjectCreateRequestDto request = createRequest(CODE, START, START, PM_ID, null);
            Project project = Project.builder()
                    .id(PROJECT_ID)
                    .code(CODE)
                    .name(NAME)
                    .startDate(START)
                    .endDate(START)
                    .projectManagerId(PM_ID)
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(projectMapper.toEntity(request)).thenReturn(project);
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(PM_ID, employee(PM_ID, "Ada")));
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.create(request)).isSameAs(response);
            assertThat(requiredIds()).containsExactly(PM_ID);
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
        }

        @Test
        @DisplayName("rejects a duplicate code before checking dates or managers")
        void shouldRejectDuplicateCode() {
            ProjectCreateRequestDto request = createRequest(CODE, END, START, PM_ID, DM_ID);

            when(projectRepository.existsByCodeIgnoreCase(CODE)).thenReturn(true);

            assertThatThrownBy(() -> projectService.create(request))
                    .isInstanceOf(ProjectCodeAlreadyExistsException.class)
                    .hasMessageContaining(CODE);

            verify(projectRepository, never()).save(any());
            verify(employeeDirectoryService, never()).requireAll(any());
            verifyNoInteractions(projectMapper, outboxEventService);
        }

        @Test
        @DisplayName("rejects an end date that is before the start date")
        void shouldRejectEndDateBeforeStartDate() {
            ProjectCreateRequestDto request = createRequest(CODE, END, START, PM_ID, null);

            when(projectRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);

            assertThatThrownBy(() -> projectService.create(request))
                    .isInstanceOf(InvalidProjectDatesException.class);

            verify(employeeDirectoryService, never()).requireAll(any());
            verify(projectRepository, never()).save(any());
            verifyNoInteractions(projectMapper, outboxEventService);
        }

        @Test
        @DisplayName("propagates an unknown manager and does not save the project")
        void shouldPropagateUnknownManager() {
            ProjectCreateRequestDto request = createRequest(CODE, START, END, PM_ID, null);

            when(projectRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(employeeDirectoryService.requireAll(any()))
                    .thenThrow(new EmployeeReferenceNotFoundException(PM_ID));

            assertThatThrownBy(() -> projectService.create(request))
                    .isInstanceOf(EmployeeReferenceNotFoundException.class)
                    .hasMessageContaining(PM_ID.toString());

            verify(projectRepository, never()).save(any());
            verifyNoInteractions(projectMapper, outboxEventService);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates a project without a code or status change")
        void shouldUpdateWithoutStatusOrCodeChange() {
            Project project = project(ProjectStatus.ACTIVE);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .name("Apollo 2")
                    .description("Updated")
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);

            verify(projectRepository, never()).existsByCodeIgnoreCase(any());
            verify(employeeDirectoryService, never()).requireAll(any());
            verify(projectMapper).updateEntity(request, project);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("checks uniqueness when the code changes")
        void shouldReplaceCodeWhenItChanges() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder().code("PRJ-2").build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.existsByCodeIgnoreCase("PRJ-2")).thenReturn(false);
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            verify(projectMapper).updateEntity(request, project);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("skips the uniqueness check when the code matches ignoring case")
        void shouldIgnoreCodeThatMatchesIgnoringCase() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder().code("prj-1").build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            verify(projectRepository, never()).existsByCodeIgnoreCase(anyString());
        }

        @Test
        @DisplayName("skips the uniqueness check when the code is blank")
        void shouldIgnoreBlankCode() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder().code("   ").build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            verify(projectRepository, never()).existsByCodeIgnoreCase(anyString());
        }

        @Test
        @DisplayName("rejects a duplicate code before validating dates")
        void shouldRejectDuplicateCode() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .code("PRJ-2")
                    .endDate(START.minusDays(1))
                    .build();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.existsByCodeIgnoreCase("PRJ-2")).thenReturn(true);

            assertThatThrownBy(() -> projectService.update(PROJECT_ID, request))
                    .isInstanceOf(ProjectCodeAlreadyExistsException.class)
                    .hasMessageContaining("PRJ-2");

            verify(projectRepository, never()).save(any());
            verifyNoInteractions(projectMapper, outboxEventService);
        }

        @Test
        @DisplayName("rejects a requested end date that is before the stored start date")
        void shouldRejectRequestedEndBeforeExistingStart() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .endDate(START.minusDays(1))
                    .build();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.update(PROJECT_ID, request))
                    .isInstanceOf(InvalidProjectDatesException.class);

            verify(projectRepository, never()).save(any());
            verify(projectMapper, never()).updateEntity(any(), any());
        }

        @Test
        @DisplayName("rejects a requested start date that is after the stored end date")
        void shouldRejectRequestedStartAfterExistingEnd() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .startDate(END.plusDays(1))
                    .build();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.update(PROJECT_ID, request))
                    .isInstanceOf(InvalidProjectDatesException.class);

            verify(projectRepository, never()).save(any());
            verify(employeeDirectoryService, never()).requireAll(any());
        }

        @Test
        @DisplayName("accepts requested dates that replace an invalid stored range")
        void shouldAcceptRequestedDates() {
            Project project = project(ProjectStatus.PLANNED);
            project.setStartDate(END);
            project.setEndDate(START);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .startDate(START)
                    .endDate(END)
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            verify(projectMapper).updateEntity(request, project);
        }

        @Test
        @DisplayName("publishes an event when the status transition is allowed")
        void shouldPublishStatusChange() {
            Project project = project(ProjectStatus.PLANNED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .status(ProjectStatus.ACTIVE)
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            doAnswer(invocation -> {
                Project entity = invocation.getArgument(1);
                entity.setStatus(ProjectStatus.ACTIVE);
                return null;
            }).when(projectMapper).updateEntity(request, project);
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            assertThat(projectEvent(EventConstants.PROJECT_STATUS_CHANGED))
                    .containsEntry("projectId", PROJECT_ID)
                    .containsEntry("code", CODE)
                    .containsEntry("previousStatus", ProjectStatus.PLANNED)
                    .containsEntry("status", ProjectStatus.ACTIVE);
        }

        @Test
        @DisplayName("rejects a forbidden status transition after the project is saved")
        void shouldRejectForbiddenStatusChange() {
            Project project = project(ProjectStatus.ACTIVE);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .status(ProjectStatus.PLANNED)
                    .build();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);

            assertThatThrownBy(() -> projectService.update(PROJECT_ID, request))
                    .isInstanceOf(InvalidProjectStatusTransitionException.class)
                    .hasMessageContaining(ProjectStatus.ACTIVE.name())
                    .hasMessageContaining(ProjectStatus.PLANNED.name());

            verify(projectRepository).save(project);
            verify(projectMapper).updateEntity(request, project);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("does not publish an event when the status stays the same")
        void shouldIgnoreUnchangedStatus() {
            Project project = project(ProjectStatus.COMPLETED);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .status(ProjectStatus.COMPLETED)
                    .name("Done")
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("throws when the project does not exist")
        void shouldThrowWhenProjectIsMissing() {
            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.update(PROJECT_ID, ProjectUpdateRequestDto.builder().build()))
                    .isInstanceOf(ProjectNotFoundException.class)
                    .hasMessageContaining(PROJECT_ID.toString());

            verifyNoInteractions(projectMapper, outboxEventService, employeeDirectoryService);
        }

        @Test
        @DisplayName("requires every manager id supplied in the request")
        void shouldValidateRequestedManagers() {
            Project project = project(ProjectStatus.ACTIVE);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .projectManagerId(PM_ID)
                    .deliveryManagerId(DM_ID)
                    .build();
            ProjectResponseDto response = response();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectMapper.toResponseDto(project)).thenReturn(response);

            assertThat(projectService.update(PROJECT_ID, request)).isSameAs(response);
            assertThat(requiredIds()).containsExactly(PM_ID, DM_ID);
        }

        @Test
        @DisplayName("propagates an unknown manager and does not save")
        void shouldPropagateUnknownManager() {
            Project project = project(ProjectStatus.ACTIVE);
            ProjectUpdateRequestDto request = ProjectUpdateRequestDto.builder()
                    .deliveryManagerId(DM_ID)
                    .build();

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
            when(employeeDirectoryService.requireAll(any()))
                    .thenThrow(new EmployeeReferenceNotFoundException(DM_ID));

            assertThatThrownBy(() -> projectService.update(PROJECT_ID, request))
                    .isInstanceOf(EmployeeReferenceNotFoundException.class)
                    .hasMessageContaining(DM_ID.toString());

            verify(projectMapper, never()).updateEntity(any(), any());
            verify(projectRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("throws when the project does not exist")
        void shouldThrowWhenProjectIsMissing() {
            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.changeStatus(PROJECT_ID, statusRequest(ProjectStatus.ACTIVE, 1L)))
                    .isInstanceOf(ProjectNotFoundException.class)
                    .hasMessageContaining(PROJECT_ID.toString());

            verifyNoInteractions(projectMapper, outboxEventService);
        }

        @Test
        @DisplayName("rejects a stale expected version")
        void shouldRejectStaleVersion() {
            Project project = project(ProjectStatus.PLANNED);
            project.setVersion(1L);
            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.changeStatus(PROJECT_ID, statusRequest(ProjectStatus.ACTIVE, 2L)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
            verify(projectRepository, never()).save(any());
            verifyNoInteractions(projectMapper, outboxEventService);
        }

        @Test
        @DisplayName("rejects a stale version even when the status does not change")
        void shouldRejectStaleVersionWhenStatusIsUnchanged() {
            Project project = project(ProjectStatus.ACTIVE);
            project.setVersion(1L);
            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.changeStatus(PROJECT_ID, statusRequest(ProjectStatus.ACTIVE, 9L)))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows a missing expected version")
        void shouldAllowMissingExpectedVersion() {
            Project project = project(ProjectStatus.PLANNED);
            project.setVersion(5L);
            ProjectResponseDto response = response();

            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            stubResponse(project, response);

            ProjectResponseDto result = projectService.changeStatus(
                    PROJECT_ID,
                    statusRequest(ProjectStatus.CANCELLED, null)
            );

            assertThat(result).isSameAs(response);
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.CANCELLED);
            assertThat(projectEvent(EventConstants.PROJECT_STATUS_CHANGED))
                    .containsEntry("previousStatus", ProjectStatus.PLANNED)
                    .containsEntry("status", ProjectStatus.CANCELLED);
        }

        @Test
        @DisplayName("returns the current project when the status does not change")
        void shouldReturnUnchangedStatus() {
            Project project = project(ProjectStatus.COMPLETED);
            ProjectResponseDto response = response();

            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));
            stubResponse(project, response);

            ProjectResponseDto result = projectService.changeStatus(
                    PROJECT_ID,
                    statusRequest(ProjectStatus.COMPLETED, 1L)
            );

            assertThat(result).isSameAs(response);
            assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
            verify(projectRepository, never()).save(any());
            verifyNoInteractions(outboxEventService);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "PLANNED, ACTIVE",
                "PLANNED, CANCELLED",
                "ACTIVE, ON_HOLD",
                "ACTIVE, COMPLETED",
                "ACTIVE, CANCELLED",
                "ON_HOLD, ACTIVE",
                "ON_HOLD, CANCELLED",
                "COMPLETED, ACTIVE",
                "CANCELLED, PLANNED"
        })
        @DisplayName("applies every allowed status transition")
        void shouldApplyAllowedTransition(ProjectStatus current, ProjectStatus target) {
            Project project = project(current);
            ProjectResponseDto response = response();

            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            stubResponse(project, response);

            ProjectResponseDto result = projectService.changeStatus(PROJECT_ID, statusRequest(target, 1L));

            assertThat(result).isSameAs(response);
            assertThat(project.getStatus()).isEqualTo(target);
            assertThat(projectEvent(EventConstants.PROJECT_STATUS_CHANGED))
                    .containsEntry("projectId", PROJECT_ID)
                    .containsEntry("code", CODE)
                    .containsEntry("previousStatus", current)
                    .containsEntry("status", target);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "PLANNED, ON_HOLD",
                "PLANNED, COMPLETED",
                "ACTIVE, PLANNED",
                "ON_HOLD, COMPLETED",
                "ON_HOLD, PLANNED",
                "COMPLETED, CANCELLED",
                "COMPLETED, ON_HOLD",
                "COMPLETED, PLANNED",
                "CANCELLED, ACTIVE",
                "CANCELLED, COMPLETED",
                "CANCELLED, ON_HOLD"
        })
        @DisplayName("rejects every forbidden status transition")
        void shouldRejectForbiddenTransition(ProjectStatus current, ProjectStatus target) {
            Project project = project(current);
            when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.changeStatus(PROJECT_ID, statusRequest(target, 1L)))
                    .isInstanceOf(InvalidProjectStatusTransitionException.class)
                    .hasMessageContaining(current.name())
                    .hasMessageContaining(target.name());

            assertThat(project.getStatus()).isEqualTo(current);
            verify(projectRepository, never()).save(any());
            verifyNoInteractions(outboxEventService, projectMapper, employeeDirectoryService);
        }
    }

    private void stubCounts(UUID projectId) {
        when(projectAssignmentRepository.countByProjectId(projectId)).thenReturn(MEMBER_COUNT);
        when(projectTaskRepository.countByProjectIdAndArchivedAtIsNull(projectId)).thenReturn(TASK_COUNT);
    }

    private void stubResponse(Project project, ProjectResponseDto response) {
        when(projectMapper.toResponseDto(project)).thenReturn(response);
        when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
        stubCounts(project.getId());
    }

    private Map<String, Object> projectEvent(String eventType) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventService).saveEvent(
                eq(PROJECT_ID),
                eq(EventConstants.AGGREGATE_PROJECT),
                eq(eventType),
                eq(EventConstants.DEFAULT_EVENT_VERSION),
                payloadCaptor.capture()
        );
        return payload(payloadCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    private Collection<UUID> resolvedIds() {
        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDirectoryService).resolveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Collection<UUID> requiredIds() {
        ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeDirectoryService).requireAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(Object value) {
        return (Map<String, Object>) value;
    }

    private static ProjectCreateRequestDto createRequest(
            String code,
            LocalDate startDate,
            LocalDate endDate,
            UUID projectManagerId,
            UUID deliveryManagerId
    ) {
        return ProjectCreateRequestDto.builder()
                .code(code)
                .name(NAME)
                .description("Landing program")
                .startDate(startDate)
                .endDate(endDate)
                .projectManagerId(projectManagerId)
                .deliveryManagerId(deliveryManagerId)
                .build();
    }

    private static ProjectStatusUpdateRequestDto statusRequest(ProjectStatus status, Long expectedVersion) {
        return ProjectStatusUpdateRequestDto.builder()
                .status(status)
                .expectedVersion(expectedVersion)
                .build();
    }

    private static Project project(ProjectStatus status) {
        return Project.builder()
                .id(PROJECT_ID)
                .code(CODE)
                .name(NAME)
                .description("Landing program")
                .status(status)
                .startDate(START)
                .endDate(END)
                .version(1L)
                .build();
    }

    private static Project managedProject(UUID projectManagerId, UUID deliveryManagerId) {
        Project project = project(ProjectStatus.ACTIVE);
        project.setProjectManagerId(projectManagerId);
        project.setDeliveryManagerId(deliveryManagerId);
        return project;
    }

    private static ProjectResponseDto response() {
        return ProjectResponseDto.builder()
                .id(PROJECT_ID)
                .code(CODE)
                .name(NAME)
                .build();
    }

    private static EmployeeReferenceDto employee(UUID id, String fullName) {
        return EmployeeReferenceDto.builder()
                .employeeId(id)
                .fullName(fullName)
                .active(true)
                .build();
    }
}
