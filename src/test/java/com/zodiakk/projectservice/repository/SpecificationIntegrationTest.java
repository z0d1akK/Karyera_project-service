package com.zodiakk.projectservice.repository;

import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectAssignmentFilterDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectFilterDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectMilestoneFilterDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectRoleFilterDto;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectMilestoneRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
import com.zodiakk.projectservice.project.repository.specification.project.ProjectSpecification;
import com.zodiakk.projectservice.project.repository.specification.projectAssignment.ProjectAssignmentSpecification;
import com.zodiakk.projectservice.project.repository.specification.projectMilestone.ProjectMilestoneSpecification;
import com.zodiakk.projectservice.project.repository.specification.projectRole.ProjectRoleSpecification;
import com.zodiakk.projectservice.task.dto.request.filter.ProjectTaskFilterDto;
import com.zodiakk.projectservice.task.dto.request.filter.TaskTypeFilterDto;
import com.zodiakk.projectservice.task.repository.ProjectTaskRepository;
import com.zodiakk.projectservice.task.repository.TaskTypeRepository;
import com.zodiakk.projectservice.task.repository.specification.projectTask.ProjectTaskSpecification;
import com.zodiakk.projectservice.task.repository.specification.taskType.TaskTypeSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Specifications")
class SpecificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Autowired
    private ProjectMilestoneRepository projectMilestoneRepository;

    @Autowired
    private ProjectRoleRepository projectRoleRepository;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Autowired
    private TaskTypeRepository taskTypeRepository;

    @Test
    @DisplayName("executes project predicates for an empty filter, a full filter, and visibility")
    void executesProjectSpecification() {
        UUID employeeId = UUID.randomUUID();

        assertThat(projectRepository.findAll(ProjectSpecification.filter(null))).isEmpty();
        assertThat(projectRepository.findAll(ProjectSpecification.filter(ProjectFilterDto.builder()
                .code("ALP")
                .name("Alpha")
                .status(ProjectStatus.ACTIVE)
                .projectManagerId(employeeId)
                .deliveryManagerId(UUID.randomUUID())
                .startDateFrom(LocalDate.of(2026, 1, 1))
                .startDateTo(LocalDate.of(2026, 6, 30))
                .endDateFrom(LocalDate.of(2026, 7, 1))
                .endDateTo(LocalDate.of(2026, 12, 31))
                .build()))).isEmpty();
        assertThat(projectRepository.findAll(ProjectSpecification.visibleTo(employeeId))).isEmpty();
    }

    @Test
    @DisplayName("executes assignment predicates for an empty filter and a full filter")
    void executesAssignmentSpecification() {
        assertThat(projectAssignmentRepository.findAll(ProjectAssignmentSpecification.filter(null))).isEmpty();
        assertThat(projectAssignmentRepository.findAll(ProjectAssignmentSpecification.filter(
                ProjectAssignmentFilterDto.builder()
                        .projectId(UUID.randomUUID())
                        .employeeId(UUID.randomUUID())
                        .projectRoleId(UUID.randomUUID())
                        .startDateFrom(LocalDate.of(2026, 1, 1))
                        .startDateTo(LocalDate.of(2026, 3, 31))
                        .endDateFrom(LocalDate.of(2026, 4, 1))
                        .endDateTo(LocalDate.of(2026, 12, 31))
                        .build()
        ))).isEmpty();
    }

    @Test
    @DisplayName("executes milestone predicates for an empty filter and a full filter")
    void executesMilestoneSpecification() {
        assertThat(projectMilestoneRepository.findAll(ProjectMilestoneSpecification.filter(null))).isEmpty();
        assertThat(projectMilestoneRepository.findAll(ProjectMilestoneSpecification.filter(
                ProjectMilestoneFilterDto.builder()
                        .projectId(UUID.randomUUID())
                        .status("OPEN")
                        .dueDateFrom(LocalDate.of(2026, 1, 1))
                        .dueDateTo(LocalDate.of(2026, 12, 31))
                        .build()
        ))).isEmpty();
    }

    @Test
    @DisplayName("executes project role predicates for an empty filter and a full filter")
    void executesProjectRoleSpecification() {
        assertThat(projectRoleRepository.findAll(ProjectRoleSpecification.filter(null))).isEmpty();
        assertThat(projectRoleRepository.findAll(ProjectRoleSpecification.filter(ProjectRoleFilterDto.builder()
                .code("DEV")
                .name("Developer")
                .active(true)
                .build()))).isEmpty();
    }

    @Test
    @DisplayName("executes task predicates for an empty filter and a full filter")
    void executesProjectTaskSpecification() {
        assertThat(projectTaskRepository.findAll(ProjectTaskSpecification.filter(null))).isEmpty();
        assertThat(projectTaskRepository.findAll(ProjectTaskSpecification.filter(ProjectTaskFilterDto.builder()
                .projectId(UUID.randomUUID())
                .columnId(UUID.randomUUID())
                .taskTypeId(UUID.randomUUID())
                .parentTaskId(UUID.randomUUID())
                .priority(TaskPriority.HIGH)
                .complexity(TaskComplexity.MEDIUM)
                .taskNumber(1L)
                .archived(false)
                .dueDateFrom(LocalDate.of(2026, 1, 1))
                .dueDateTo(LocalDate.of(2026, 12, 31))
                .search("kickoff")
                .assigneeAssignmentId(UUID.randomUUID())
                .build()))).isEmpty();
    }

    @Test
    @DisplayName("executes task type predicates for an empty filter and a full filter")
    void executesTaskTypeSpecification() {
        assertThat(taskTypeRepository.findAll(TaskTypeSpecification.filter(null))).isEmpty();
        assertThat(taskTypeRepository.findAll(TaskTypeSpecification.filter(TaskTypeFilterDto.builder()
                .code("STORY")
                .name("Story")
                .active(true)
                .build()))).isEmpty();
    }
}
