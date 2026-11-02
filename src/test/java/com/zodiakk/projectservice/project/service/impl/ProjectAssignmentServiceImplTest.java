package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.constants.EventConstants;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceInactiveException;
import com.zodiakk.projectservice.integration.service.EmployeeDirectoryService;
import com.zodiakk.projectservice.outbox.service.OutboxEventService;
import com.zodiakk.projectservice.project.dto.request.action.ProjectAssignmentEndRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectAssignmentFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectAssignmentUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectAssignmentResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;
import com.zodiakk.projectservice.project.entity.ProjectRole;
import com.zodiakk.projectservice.project.exception.assignment.InvalidAllocationPercentException;
import com.zodiakk.projectservice.project.exception.assignment.InvalidAssignmentDatesException;
import com.zodiakk.projectservice.project.exception.assignment.ProjectAssignmentAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.assignment.ProjectAssignmentNotFoundException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleInactiveException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectAssignmentMapper;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
import com.zodiakk.projectservice.security.access.ProjectAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAssignmentServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID ASSIGNMENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID SECOND_ASSIGNMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final UUID EMPLOYEE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final UUID SECOND_EMPLOYEE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private static final UUID CURRENT_EMPLOYEE_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID ROLE_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private static final UUID NEW_ROLE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private static final LocalDate START = LocalDate.of(2026, 1, 10);

    private static final LocalDate END = LocalDate.of(2026, 6, 1);

    private static final BigDecimal ALLOCATION = new BigDecimal("50.00");

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectAssignmentMapper projectAssignmentMapper;

    @Mock
    private OutboxEventService outboxEventService;

    @Mock
    private EmployeeDirectoryService employeeDirectoryService;

    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private ProjectAssignmentServiceImpl projectAssignmentService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns an empty page when the caller cannot view the project")
        void shouldReturnEmptyPageWhenProjectIsHidden() {
            ProjectAssignmentFilterDto filter = filter(PROJECT_ID, SECOND_EMPLOYEE_ID);

            when(projectAccessService.canViewProject(PROJECT_ID)).thenReturn(false);

            Page<ProjectAssignmentResponseDto> result = projectAssignmentService.findAll(filter, pageRequest());

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(filter.getEmployeeId()).isEqualTo(SECOND_EMPLOYEE_ID);
            verify(projectAccessService, never()).canViewAllProjects();
            verifyNoInteractions(
                    projectAssignmentRepository,
                    projectRoleRepository,
                    projectAssignmentMapper,
                    employeeDirectoryService
            );
        }

        @Test
        @DisplayName("maps employees and roles for a visible project")
        void shouldMapAssignmentsForAVisibleProject() {
            ProjectAssignmentFilterDto filter = filter(PROJECT_ID, SECOND_EMPLOYEE_ID);
            ProjectAssignment resolved = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            ProjectAssignment unresolved = assignment(SECOND_ASSIGNMENT_ID, SECOND_EMPLOYEE_ID, null, null);
            ProjectRole role = role(ROLE_ID, "Developer", true);
            EmployeeReferenceDto employee = employee(EMPLOYEE_ID, "Ada Lovelace", true);
            ProjectAssignmentResponseDto resolvedResponse = response(ASSIGNMENT_ID);
            ProjectAssignmentResponseDto unresolvedResponse = response(SECOND_ASSIGNMENT_ID);
            unresolvedResponse.setEmployeeFullName("preset employee");
            unresolvedResponse.setProjectRoleName("preset role");
            Page<ProjectAssignment> page = new PageImpl<>(
                    List.of(resolved, unresolved),
                    PageRequest.of(1, 5),
                    12
            );

            when(projectAccessService.canViewProject(PROJECT_ID)).thenReturn(true);
            when(projectAssignmentRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(EMPLOYEE_ID, employee));
            when(projectRoleRepository.findAllById(any())).thenReturn(List.of(role));
            when(projectAssignmentMapper.toResponseDto(resolved)).thenReturn(resolvedResponse);
            when(projectAssignmentMapper.toResponseDto(unresolved)).thenReturn(unresolvedResponse);

            Page<ProjectAssignmentResponseDto> result = projectAssignmentService.findAll(filter, pageRequest());

            assertThat(result.getContent()).containsExactly(resolvedResponse, unresolvedResponse);
            assertThat(result.getTotalElements()).isEqualTo(12);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(resolvedResponse.getEmployeeFullName()).isEqualTo("Ada Lovelace");
            assertThat(resolvedResponse.getProjectRoleName()).isEqualTo("Developer");
            assertThat(unresolvedResponse.getEmployeeFullName()).isEqualTo("preset employee");
            assertThat(unresolvedResponse.getProjectRoleName()).isEqualTo("preset role");
            assertThat(filter.getEmployeeId()).isEqualTo(SECOND_EMPLOYEE_ID);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectAssignmentRepository).findAll(any(Specification.class), pageableCaptor.capture());
            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("startDate")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("startDate").getDirection()).isEqualTo(Sort.Direction.DESC);
            verify(projectAccessService, never()).canViewAllProjects();
        }

        @Test
        @DisplayName("forces the filter to the current employee when the caller cannot view all projects")
        void shouldLimitResultsToTheCurrentEmployee() {
            ProjectAssignmentFilterDto filter = filter(null, SECOND_EMPLOYEE_ID);
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, CURRENT_EMPLOYEE_ID, ROLE_ID, null);
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            when(projectAccessService.canViewAllProjects()).thenReturn(false);
            when(employeeDirectoryService.getCurrentEmployee())
                    .thenReturn(employee(CURRENT_EMPLOYEE_ID, "Current", true));
            when(projectAssignmentRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(employeeDirectoryService.resolveAll(any()))
                    .thenReturn(Map.of(CURRENT_EMPLOYEE_ID, employee(CURRENT_EMPLOYEE_ID, "Current", true)));
            when(projectRoleRepository.findAllById(any())).thenReturn(List.of(role(ROLE_ID, "Developer", true)));
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            Page<ProjectAssignmentResponseDto> result = projectAssignmentService.findAll(filter, pageRequest());

            assertThat(result.getContent()).containsExactly(expected);
            assertThat(filter.getEmployeeId()).isEqualTo(CURRENT_EMPLOYEE_ID);
            assertThat(expected.getEmployeeFullName()).isEqualTo("Current");
            assertThat(expected.getProjectRoleName()).isEqualTo("Developer");
            verify(projectAccessService, never()).canViewProject(any());
        }

        @Test
        @DisplayName("keeps the requested employee when the caller can view all projects")
        void shouldKeepRequestedEmployeeWhenCallerCanViewAll() {
            ProjectAssignmentFilterDto filter = filter(null, SECOND_EMPLOYEE_ID);

            when(projectAccessService.canViewAllProjects()).thenReturn(true);
            when(projectAssignmentRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectRoleRepository.findAllById(any())).thenReturn(List.of());

            Page<ProjectAssignmentResponseDto> result = projectAssignmentService.findAll(filter, pageRequest());

            assertThat(result.getContent()).isEmpty();
            assertThat(filter.getEmployeeId()).isEqualTo(SECOND_EMPLOYEE_ID);
            verify(employeeDirectoryService, never()).getCurrentEmployee();
            verify(projectAssignmentMapper, never()).toResponseDto(any());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("creates an open assignment and publishes an event")
        void shouldCreateAssignment() {
            ProjectAssignmentCreateRequestDto request = createRequest(START, END, ALLOCATION);
            request.setProjectId(OTHER_PROJECT_ID);
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, END);
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);
            EmployeeReferenceDto employee = employee(EMPLOYEE_ID, "Ada Lovelace", true);

            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", true)));
            when(employeeDirectoryService.requireById(EMPLOYEE_ID)).thenReturn(employee);
            stubNoOpenAssignment();
            when(projectAssignmentMapper.toEntity(request)).thenReturn(entity);
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectAssignmentResponseDto result = projectAssignmentService.create(PROJECT_ID, request);

            assertThat(result).isSameAs(expected);
            assertThat(request.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(result.getEmployeeFullName()).isEqualTo("Ada Lovelace");
            assertThat(result.getProjectRoleName()).isEqualTo("Developer");
            assertThat(assignmentEvent(EventConstants.PROJECT_ASSIGNMENT_CREATED))
                    .containsEntry("assignmentId", ASSIGNMENT_ID)
                    .containsEntry("projectId", PROJECT_ID)
                    .containsEntry("employeeId", EMPLOYEE_ID)
                    .containsEntry("projectRoleId", ROLE_ID)
                    .containsEntry("startDate", START)
                    .containsEntry("endDate", END)
                    .containsEntry("allocationPercent", ALLOCATION);
        }

        @Test
        @DisplayName("creates an assignment without an end date or allocation")
        void shouldCreateOpenEndedAssignmentWithoutAllocation() {
            ProjectAssignmentCreateRequestDto request = createRequest(START, null, null);
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            entity.setAllocationPercent(null);
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            stubReadyToSave(request, entity, expected);

            assertThat(projectAssignmentService.create(PROJECT_ID, request)).isSameAs(expected);
            assertThat(assignmentEvent(EventConstants.PROJECT_ASSIGNMENT_CREATED))
                    .containsEntry("endDate", null)
                    .containsEntry("allocationPercent", null);
        }

        @Test
        @DisplayName("creates an assignment when the start date is missing")
        void shouldAllowMissingStartDate() {
            ProjectAssignmentCreateRequestDto request = createRequest(null, END, ALLOCATION);
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, END);
            entity.setStartDate(null);
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            stubReadyToSave(request, entity, expected);

            assertThat(projectAssignmentService.create(PROJECT_ID, request)).isSameAs(expected);
        }

        @ParameterizedTest(name = "allocation {0}")
        @ValueSource(strings = {"0", "0.00", "100", "100.00", "42.5"})
        @DisplayName("accepts allocation inside the inclusive range")
        void shouldAcceptAllocation(String raw) {
            BigDecimal allocation = new BigDecimal(raw);
            ProjectAssignmentCreateRequestDto request = createRequest(START, END, allocation);
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, END);
            entity.setAllocationPercent(allocation);
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            stubReadyToSave(request, entity, expected);

            assertThat(projectAssignmentService.create(PROJECT_ID, request)).isSameAs(expected);
        }

        @Test
        @DisplayName("throws when the project does not exist")
        void shouldThrowWhenProjectIsMissing() {
            ProjectAssignmentCreateRequestDto request = createRequest(START, END, ALLOCATION);
            request.setProjectId(OTHER_PROJECT_ID);

            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, request))
                    .isInstanceOf(ProjectNotFoundException.class)
                    .hasMessageContaining(PROJECT_ID.toString());

            assertThat(request.getProjectId()).isEqualTo(PROJECT_ID);
            verifyNoInteractions(
                    projectRoleRepository,
                    employeeDirectoryService,
                    projectAssignmentMapper,
                    outboxEventService
            );
            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the role does not exist")
        void shouldThrowWhenRoleIsMissing() {
            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, createRequest(START, END, ALLOCATION)))
                    .isInstanceOf(ProjectRoleNotFoundException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verifyNoInteractions(employeeDirectoryService, projectAssignmentMapper, outboxEventService);
        }

        @Test
        @DisplayName("throws when the role is inactive")
        void shouldThrowWhenRoleIsInactive() {
            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", false)));

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, createRequest(START, END, ALLOCATION)))
                    .isInstanceOf(ProjectRoleInactiveException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verifyNoInteractions(employeeDirectoryService, projectAssignmentMapper);
        }

        @Test
        @DisplayName("throws when the role has no active flag")
        void shouldThrowWhenRoleActiveFlagIsNull() {
            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", null)));

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, createRequest(START, END, ALLOCATION)))
                    .isInstanceOf(ProjectRoleInactiveException.class)
                    .hasMessageContaining(ROLE_ID.toString());
        }

        @Test
        @DisplayName("throws when the employee is inactive")
        void shouldThrowWhenEmployeeIsInactive() {
            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", true)));
            when(employeeDirectoryService.requireById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, "Ada", false));

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, createRequest(START, END, ALLOCATION)))
                    .isInstanceOf(EmployeeReferenceInactiveException.class)
                    .hasMessageContaining(EMPLOYEE_ID.toString());

            verify(projectAssignmentRepository, never()).existsByProjectIdAndEmployeeIdAndEndDateIsNull(any(), any());
            verifyNoInteractions(projectAssignmentMapper, outboxEventService);
        }

        @Test
        @DisplayName("throws when the employee has no active flag")
        void shouldThrowWhenEmployeeActiveFlagIsNull() {
            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", true)));
            when(employeeDirectoryService.requireById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, "Ada", null));

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, createRequest(START, END, ALLOCATION)))
                    .isInstanceOf(EmployeeReferenceInactiveException.class)
                    .hasMessageContaining(EMPLOYEE_ID.toString());
        }

        @Test
        @DisplayName("rejects an employee who already has an open assignment")
        void shouldRejectDuplicateOpenAssignment() {
            ProjectAssignmentCreateRequestDto request = createRequest(END, START, ALLOCATION);

            stubProjectExists();
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", true)));
            when(employeeDirectoryService.requireById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, "Ada", true));
            when(projectAssignmentRepository.existsByProjectIdAndEmployeeIdAndEndDateIsNull(PROJECT_ID, EMPLOYEE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, request))
                    .isInstanceOf(ProjectAssignmentAlreadyExistsException.class)
                    .hasMessageContaining(EMPLOYEE_ID.toString())
                    .hasMessageContaining(PROJECT_ID.toString());

            verify(projectAssignmentRepository, never()).save(any());
            verifyNoInteractions(projectAssignmentMapper, outboxEventService);
        }

        @Test
        @DisplayName("rejects an end date that is before the start date")
        void shouldRejectEndDateBeforeStartDate() {
            ProjectAssignmentCreateRequestDto request = createRequest(END, START, new BigDecimal("150"));

            stubCreatePreconditions();

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, request))
                    .isInstanceOf(InvalidAssignmentDatesException.class);

            verify(projectAssignmentRepository, never()).save(any());
            verifyNoInteractions(projectAssignmentMapper, outboxEventService);
        }

        @ParameterizedTest(name = "allocation {0}")
        @ValueSource(strings = {"-0.01", "-1", "100.01", "150"})
        @DisplayName("rejects allocation outside 0 to 100")
        void shouldRejectAllocation(String raw) {
            BigDecimal allocation = new BigDecimal(raw);
            ProjectAssignmentCreateRequestDto request = createRequest(START, END, allocation);

            stubCreatePreconditions();

            assertThatThrownBy(() -> projectAssignmentService.create(PROJECT_ID, request))
                    .isInstanceOf(InvalidAllocationPercentException.class)
                    .hasMessageContaining(allocation.toString());

            verify(projectAssignmentRepository, never()).save(any());
            verifyNoInteractions(projectAssignmentMapper, outboxEventService);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("changes the role and enriches the response")
        void shouldUpdateAssignment() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            ProjectRole currentRole = role(ROLE_ID, "Developer", false);
            ProjectRole newRole = role(NEW_ROLE_ID, "Lead", true);
            ProjectAssignmentUpdateRequestDto request = ProjectAssignmentUpdateRequestDto.builder()
                    .projectRoleId(NEW_ROLE_ID)
                    .startDate(START)
                    .endDate(END)
                    .allocationPercent(new BigDecimal("100"))
                    .build();
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);
            EmployeeReferenceDto employee = employee(EMPLOYEE_ID, "Ada Lovelace", true);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(currentRole));
            when(projectRoleRepository.findById(NEW_ROLE_ID)).thenReturn(Optional.of(newRole));
            applyUpdate(request, entity);
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of(EMPLOYEE_ID, employee));
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectAssignmentResponseDto result = projectAssignmentService.update(ASSIGNMENT_ID, request);

            assertThat(result).isSameAs(expected);
            assertThat(result.getEmployeeFullName()).isEqualTo("Ada Lovelace");
            assertThat(result.getProjectRoleName()).isEqualTo("Lead");
            verify(projectAssignmentMapper).updateEntity(request, entity);
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("keeps an inactive current role when the request does not change it")
        void shouldKeepInactiveRoleWhenItIsUnchanged() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, END);
            ProjectRole currentRole = role(ROLE_ID, "Developer", false);
            ProjectAssignmentUpdateRequestDto request = ProjectAssignmentUpdateRequestDto.builder()
                    .allocationPercent(ALLOCATION)
                    .build();
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(currentRole));
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectAssignmentResponseDto result = projectAssignmentService.update(ASSIGNMENT_ID, request);

            assertThat(result.getProjectRoleName()).isEqualTo("Developer");
            assertThat(result.getEmployeeFullName()).isNull();
        }

        @Test
        @DisplayName("assigns a role when the assignment had none")
        void shouldAssignRoleWhenAssignmentHadNone() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, null);
            ProjectRole newRole = role(NEW_ROLE_ID, "Lead", true);
            ProjectAssignmentUpdateRequestDto request = ProjectAssignmentUpdateRequestDto.builder()
                    .projectRoleId(NEW_ROLE_ID)
                    .build();
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(NEW_ROLE_ID)).thenReturn(Optional.of(newRole));
            applyUpdate(request, entity);
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectAssignmentService.update(ASSIGNMENT_ID, request).getProjectRoleName()).isEqualTo("Lead");
            verify(projectRoleRepository, never()).findById(ROLE_ID);
        }

        @Test
        @DisplayName("throws when the assignment does not exist")
        void shouldThrowWhenMissing() {
            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectAssignmentService.update(
                    ASSIGNMENT_ID,
                    ProjectAssignmentUpdateRequestDto.builder().build()
            ))
                    .isInstanceOf(ProjectAssignmentNotFoundException.class)
                    .hasMessageContaining(ASSIGNMENT_ID.toString());

            verifyNoInteractions(projectRoleRepository, projectAssignmentMapper, employeeDirectoryService);
        }

        @Test
        @DisplayName("throws when the current role does not exist")
        void shouldThrowWhenCurrentRoleIsMissing() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            ProjectAssignmentUpdateRequestDto request = ProjectAssignmentUpdateRequestDto.builder()
                    .projectRoleId(NEW_ROLE_ID)
                    .build();

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectAssignmentService.update(ASSIGNMENT_ID, request))
                    .isInstanceOf(ProjectRoleNotFoundException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verify(projectRoleRepository, never()).findById(NEW_ROLE_ID);
            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the requested role does not exist")
        void shouldThrowWhenRequestedRoleIsMissing() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", true)));
            when(projectRoleRepository.findById(NEW_ROLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectAssignmentService.update(
                    ASSIGNMENT_ID,
                    ProjectAssignmentUpdateRequestDto.builder().projectRoleId(NEW_ROLE_ID).build()
            ))
                    .isInstanceOf(ProjectRoleNotFoundException.class)
                    .hasMessageContaining(NEW_ROLE_ID.toString());
        }

        @Test
        @DisplayName("throws when the requested role is inactive")
        void shouldThrowWhenRequestedRoleIsInactive() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, null);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(NEW_ROLE_ID)).thenReturn(Optional.of(role(NEW_ROLE_ID, "Lead", false)));

            assertThatThrownBy(() -> projectAssignmentService.update(
                    ASSIGNMENT_ID,
                    ProjectAssignmentUpdateRequestDto.builder().projectRoleId(NEW_ROLE_ID).build()
            ))
                    .isInstanceOf(ProjectRoleInactiveException.class)
                    .hasMessageContaining(NEW_ROLE_ID.toString());

            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the requested role has no active flag")
        void shouldThrowWhenRequestedRoleActiveFlagIsNull() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, null);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.findById(NEW_ROLE_ID)).thenReturn(Optional.of(role(NEW_ROLE_ID, "Lead", null)));

            assertThatThrownBy(() -> projectAssignmentService.update(
                    ASSIGNMENT_ID,
                    ProjectAssignmentUpdateRequestDto.builder().projectRoleId(NEW_ROLE_ID).build()
            ))
                    .isInstanceOf(ProjectRoleInactiveException.class);
        }

        @Test
        @DisplayName("rejects a requested end date that is before the stored start date")
        void shouldRejectRequestedEndBeforeExistingStart() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, null);
            ProjectAssignmentUpdateRequestDto request = ProjectAssignmentUpdateRequestDto.builder()
                    .endDate(START.minusDays(1))
                    .allocationPercent(new BigDecimal("150"))
                    .build();

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> projectAssignmentService.update(ASSIGNMENT_ID, request))
                    .isInstanceOf(InvalidAssignmentDatesException.class);

            verify(projectAssignmentMapper, never()).updateEntity(any(), any());
            verify(projectAssignmentRepository, never()).save(any());
            verifyNoInteractions(employeeDirectoryService);
        }

        @Test
        @DisplayName("rejects a requested start date that is after the stored end date")
        void shouldRejectRequestedStartAfterExistingEnd() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, END);

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> projectAssignmentService.update(
                    ASSIGNMENT_ID,
                    ProjectAssignmentUpdateRequestDto.builder().startDate(END.plusDays(1)).build()
            ))
                    .isInstanceOf(InvalidAssignmentDatesException.class);

            verify(projectAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects allocation outside 0 to 100")
        void shouldRejectAllocation() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, null);
            BigDecimal allocation = new BigDecimal("-1");

            when(projectAssignmentRepository.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> projectAssignmentService.update(
                    ASSIGNMENT_ID,
                    ProjectAssignmentUpdateRequestDto.builder().allocationPercent(allocation).build()
            ))
                    .isInstanceOf(InvalidAllocationPercentException.class)
                    .hasMessageContaining(allocation.toString());

            verify(projectAssignmentMapper, never()).updateEntity(any(), any());
        }
    }

    @Nested
    @DisplayName("end")
    class End {

        @Test
        @DisplayName("ends an open assignment and publishes an event")
        void shouldEndAssignment() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            ProjectAssignmentEndRequestDto request = ProjectAssignmentEndRequestDto.builder()
                    .endDate(END)
                    .comment("Finished")
                    .expectedVersion(99L)
                    .build();
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            when(projectAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(employeeDirectoryService.resolveAll(any()))
                    .thenReturn(Map.of(EMPLOYEE_ID, employee(EMPLOYEE_ID, "Ada Lovelace", true)));
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectAssignmentResponseDto result = projectAssignmentService.end(ASSIGNMENT_ID, request);

            assertThat(result).isSameAs(expected);
            assertThat(entity.getEndDate()).isEqualTo(END);
            assertThat(result.getEmployeeFullName()).isEqualTo("Ada Lovelace");
            assertThat(result.getProjectRoleName()).isNull();
            assertThat(assignmentEvent(EventConstants.PROJECT_ASSIGNMENT_ENDED))
                    .containsEntry("assignmentId", ASSIGNMENT_ID)
                    .containsEntry("projectId", PROJECT_ID)
                    .containsEntry("employeeId", EMPLOYEE_ID)
                    .containsEntry("endDate", END)
                    .containsEntry("allocationPercent", ALLOCATION);
        }

        @Test
        @DisplayName("uses today when the request omits the end date")
        void shouldDefaultEndDateToToday() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            entity.setStartDate(LocalDate.of(2020, 1, 1));
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            when(projectAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            projectAssignmentService.end(ASSIGNMENT_ID, ProjectAssignmentEndRequestDto.builder().build());

            assertThat(entity.getEndDate()).isEqualTo(LocalDate.now());
            verify(outboxEventService).saveEvent(
                    eq(ASSIGNMENT_ID),
                    eq(EventConstants.AGGREGATE_PROJECT_ASSIGNMENT),
                    eq(EventConstants.PROJECT_ASSIGNMENT_ENDED),
                    eq(EventConstants.DEFAULT_EVENT_VERSION),
                    any()
            );
        }

        @Test
        @DisplayName("ends an assignment whose start date is missing")
        void shouldAllowEndingWhenStartDateIsMissing() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, null);
            entity.setStartDate(null);
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);

            when(projectAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(projectAssignmentRepository.save(entity)).thenReturn(entity);
            when(employeeDirectoryService.resolveAll(any())).thenReturn(Map.of());
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            projectAssignmentService.end(
                    ASSIGNMENT_ID,
                    ProjectAssignmentEndRequestDto.builder().endDate(END).build()
            );

            assertThat(entity.getEndDate()).isEqualTo(END);
        }

        @Test
        @DisplayName("returns an already ended assignment without saving")
        void shouldReturnAlreadyEndedAssignment() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, ROLE_ID, END);
            entity.setStartDate(END.plusDays(10));
            ProjectAssignmentResponseDto expected = response(ASSIGNMENT_ID);
            expected.setProjectRoleName("preset role");

            when(projectAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));
            when(employeeDirectoryService.resolveAll(any()))
                    .thenReturn(Map.of(EMPLOYEE_ID, employee(EMPLOYEE_ID, "Ada Lovelace", true)));
            when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectAssignmentResponseDto result = projectAssignmentService.end(
                    ASSIGNMENT_ID,
                    ProjectAssignmentEndRequestDto.builder().endDate(START).build()
            );

            assertThat(result).isSameAs(expected);
            assertThat(entity.getEndDate()).isEqualTo(END);
            assertThat(result.getEmployeeFullName()).isEqualTo("Ada Lovelace");
            assertThat(result.getProjectRoleName()).isEqualTo("preset role");
            verify(projectAssignmentRepository, never()).save(any());
            verifyNoInteractions(outboxEventService);
        }

        @Test
        @DisplayName("rejects an end date that is before the start date")
        void shouldRejectEndDateBeforeStartDate() {
            ProjectAssignment entity = assignment(ASSIGNMENT_ID, EMPLOYEE_ID, null, null);

            when(projectAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> projectAssignmentService.end(
                    ASSIGNMENT_ID,
                    ProjectAssignmentEndRequestDto.builder().endDate(START.minusDays(1)).build()
            ))
                    .isInstanceOf(InvalidAssignmentDatesException.class);

            assertThat(entity.getEndDate()).isNull();
            verify(projectAssignmentRepository, never()).save(any());
            verifyNoInteractions(outboxEventService, projectAssignmentMapper, employeeDirectoryService);
        }

        @Test
        @DisplayName("throws when the assignment does not exist")
        void shouldThrowWhenMissing() {
            when(projectAssignmentRepository.findByIdForUpdate(ASSIGNMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectAssignmentService.end(
                    ASSIGNMENT_ID,
                    ProjectAssignmentEndRequestDto.builder().endDate(END).build()
            ))
                    .isInstanceOf(ProjectAssignmentNotFoundException.class)
                    .hasMessageContaining(ASSIGNMENT_ID.toString());

            verifyNoInteractions(outboxEventService, projectAssignmentMapper, employeeDirectoryService);
        }
    }

    private void applyUpdate(ProjectAssignmentUpdateRequestDto request, ProjectAssignment entity) {
        doAnswer(invocation -> {
            ProjectAssignmentUpdateRequestDto dto = invocation.getArgument(0);
            ProjectAssignment target = invocation.getArgument(1);
            if (dto.getProjectRoleId() != null) {
                target.setProjectRoleId(dto.getProjectRoleId());
            }
            if (dto.getStartDate() != null) {
                target.setStartDate(dto.getStartDate());
            }
            if (dto.getEndDate() != null) {
                target.setEndDate(dto.getEndDate());
            }
            if (dto.getAllocationPercent() != null) {
                target.setAllocationPercent(dto.getAllocationPercent());
            }
            return null;
        }).when(projectAssignmentMapper).updateEntity(request, entity);
    }

    private void stubProjectExists() {
        when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
    }

    private void stubNoOpenAssignment() {
        when(projectAssignmentRepository.existsByProjectIdAndEmployeeIdAndEndDateIsNull(PROJECT_ID, EMPLOYEE_ID))
                .thenReturn(false);
    }

    private void stubCreatePreconditions() {
        stubProjectExists();
        when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role(ROLE_ID, "Developer", true)));
        when(employeeDirectoryService.requireById(EMPLOYEE_ID)).thenReturn(employee(EMPLOYEE_ID, "Ada Lovelace", true));
        stubNoOpenAssignment();
    }

    private void stubReadyToSave(
            ProjectAssignmentCreateRequestDto request,
            ProjectAssignment entity,
            ProjectAssignmentResponseDto expected
    ) {
        stubCreatePreconditions();
        when(projectAssignmentMapper.toEntity(request)).thenReturn(entity);
        when(projectAssignmentRepository.save(entity)).thenReturn(entity);
        when(projectAssignmentMapper.toResponseDto(entity)).thenReturn(expected);
    }

    private Map<String, Object> assignmentEvent(String eventType) {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventService).saveEvent(
                eq(ASSIGNMENT_ID),
                eq(EventConstants.AGGREGATE_PROJECT_ASSIGNMENT),
                eq(eventType),
                eq(EventConstants.DEFAULT_EVENT_VERSION),
                payloadCaptor.capture()
        );
        return payload(payloadCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(Object value) {
        return (Map<String, Object>) value;
    }

    private static ProjectAssignmentFilterDto filter(UUID projectId, UUID employeeId) {
        return ProjectAssignmentFilterDto.builder()
                .projectId(projectId)
                .employeeId(employeeId)
                .projectRoleId(ROLE_ID)
                .startDateFrom(START)
                .startDateTo(END)
                .endDateFrom(START)
                .endDateTo(END)
                .build();
    }

    private static PageRequestDto pageRequest() {
        return PageRequestDto.builder()
                .page(1)
                .size(5)
                .sortBy("startDate")
                .sortDirection(SortDirection.DESC)
                .build();
    }

    private static ProjectAssignmentCreateRequestDto createRequest(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal allocation
    ) {
        return ProjectAssignmentCreateRequestDto.builder()
                .employeeId(EMPLOYEE_ID)
                .projectRoleId(ROLE_ID)
                .startDate(startDate)
                .endDate(endDate)
                .allocationPercent(allocation)
                .build();
    }

    private static ProjectAssignment assignment(
            UUID id,
            UUID employeeId,
            UUID roleId,
            LocalDate endDate
    ) {
        return ProjectAssignment.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .employeeId(employeeId)
                .projectRoleId(roleId)
                .startDate(START)
                .endDate(endDate)
                .allocationPercent(ALLOCATION)
                .version(1L)
                .build();
    }

    private static ProjectRole role(UUID id, String name, Boolean active) {
        return ProjectRole.builder()
                .id(id)
                .code(name.toUpperCase())
                .name(name)
                .active(active)
                .build();
    }

    private static EmployeeReferenceDto employee(UUID id, String fullName, Boolean active) {
        return EmployeeReferenceDto.builder()
                .employeeId(id)
                .fullName(fullName)
                .active(active)
                .build();
    }

    private static ProjectAssignmentResponseDto response(UUID id) {
        return ProjectAssignmentResponseDto.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .employeeId(EMPLOYEE_ID)
                .build();
    }
}
