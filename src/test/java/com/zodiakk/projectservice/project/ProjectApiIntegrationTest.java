package com.zodiakk.projectservice.project;

import com.zodiakk.projectservice.board.dto.request.action.BoardColumnOrderUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.BoardColumnUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import com.zodiakk.projectservice.project.dto.request.action.ProjectAssignmentEndRequestDto;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectLinkCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectMilestoneCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectAssignmentUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectLinkUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectMilestoneUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectRoleUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Project API")
class ProjectApiIntegrationTest extends AbstractIntegrationTest {

    private final UUID adminUserId = UUID.randomUUID();

    private UUID adminEmployeeId;

    private UUID memberEmployeeId;

    @BeforeEach
    void registerEmployees() {
        adminEmployeeId = registerEmployee(adminUserId).id();
        memberEmployeeId = registerEmployee(null).id();
    }

    @Override
    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asAdmin(B builder) {
        return asUser(builder, adminUserId, "ADMIN");
    }

    @Test
    @DisplayName("covers project, board, task, assignment, milestone and link endpoints")
    void coversProjectApi() throws Exception {
        JsonNode developerRole = submit(post("/api/v1/project-roles"), role("DEV", "Developer"), 201);
        JsonNode spareRole = submit(post("/api/v1/project-roles"), role("QA", "Quality analyst"), 201);
        submit(delete("/api/v1/project-roles/{id}", idOf(spareRole)), null, 204);

        JsonNode roles = submit(get("/api/v1/project-roles")
                .param("code", "DEV")
                .param("name", "Developer")
                .param("active", "true")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "name")
                .param("sortDirection", "ASC"), null, 200);
        assertThat(roles.get("content")).isNotEmpty();

        submit(get("/api/v1/project-roles/{id}", idOf(developerRole)), null, 200);
        submit(patch("/api/v1/project-roles/{id}", idOf(developerRole)),
                ProjectRoleUpdateRequestDto.builder().description("Builds the product").build(),
                200);

        JsonNode project = submit(post("/api/v1/projects"), ProjectCreateRequestDto.builder()
                .code("ALPHA")
                .name("Alpha")
                .description("Alpha delivery")
                .startDate(LocalDate.of(2026, 1, 15))
                .endDate(LocalDate.of(2026, 12, 15))
                .projectManagerId(adminEmployeeId)
                .deliveryManagerId(memberEmployeeId)
                .build(), 201);
        UUID projectId = idOf(project);

        JsonNode projects = submit(get("/api/v1/projects")
                .param("code", "ALPHA")
                .param("name", "Alpha")
                .param("status", "PLANNED")
                .param("projectManagerId", adminEmployeeId.toString())
                .param("deliveryManagerId", memberEmployeeId.toString())
                .param("startDateFrom", "2026-01-01")
                .param("startDateTo", "2026-02-01")
                .param("endDateFrom", "2026-12-01")
                .param("endDateTo", "2026-12-31")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "name")
                .param("sortDirection", "DESC"), null, 200);
        assertThat(projects.get("content")).isNotEmpty();

        submit(get("/api/v1/projects/{id}", projectId), null, 200);
        submit(patch("/api/v1/projects/{id}", projectId),
                ProjectUpdateRequestDto.builder().description("Updated delivery scope").build(),
                200);
        JsonNode currentProject = submit(get("/api/v1/projects/{id}", projectId), null, 200);
        submit(patch("/api/v1/projects/{id}/status", projectId),
                ProjectStatusUpdateRequestDto.builder()
                        .status(ProjectStatus.ACTIVE)
                        .expectedVersion(versionOf(currentProject))
                        .build(),
                200);

        JsonNode board = submit(post("/api/v1/projects/{id}/board", projectId),
                ProjectBoardUpdateRequestDto.builder().name("Delivery").build(),
                201);
        JsonNode todo = submit(post("/api/v1/projects/{id}/board/columns", projectId),
                column("TODO", "To do", 0, true, false), 201);
        JsonNode doing = submit(post("/api/v1/projects/{id}/board/columns", projectId),
                column("DOING", "Doing", 1, false, false), 201);
        JsonNode extra = submit(post("/api/v1/projects/{id}/board/columns", projectId),
                column("EXTRA", "Extra", 2, false, false), 201);

        JsonNode reordered = submit(patch("/api/v1/projects/{id}/board/columns/order", projectId),
                BoardColumnOrderUpdateRequestDto.builder()
                        .columnIds(List.of(idOf(doing), idOf(todo), idOf(extra)))
                        .build(),
                200);
        assertThat(reordered.get("columns")).hasSize(3);

        JsonNode taskType = submit(post("/api/v1/task-types"),
                TaskTypeCreateRequestDto.builder().code("TASK").name("Task").active(true).build(),
                201);
        JsonNode task = submit(post("/api/v1/projects/{id}/tasks", projectId),
                ProjectTaskCreateRequestDto.builder()
                        .title("Prepare kickoff")
                        .description("Kickoff notes")
                        .taskTypeId(idOf(taskType))
                        .columnId(idOf(todo))
                        .priority(TaskPriority.MEDIUM)
                        .complexity(TaskComplexity.MEDIUM)
                        .storyPoints(new BigDecimal("3"))
                        .estimatedMinutes(120)
                        .dueDate(LocalDate.of(2026, 6, 1))
                        .build(),
                201);

        JsonNode loadedBoard = submit(get("/api/v1/projects/{id}/board", projectId), null, 200);
        assertThat(loadedBoard.get("id").asString()).isEqualTo(idOf(board).toString());

        submit(get("/api/v1/projects/{id}/tasks", projectId)
                .param("projectId", projectId.toString())
                .param("columnId", idOf(todo).toString())
                .param("taskTypeId", idOf(taskType).toString())
                .param("parentTaskId", idOf(task).toString())
                .param("priority", "MEDIUM")
                .param("complexity", "MEDIUM")
                .param("taskNumber", "1")
                .param("archived", "false")
                .param("dueDateFrom", "2026-05-01")
                .param("dueDateTo", "2026-07-01")
                .param("search", "kickoff")
                .param("assigneeAssignmentId", UUID.randomUUID().toString())
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "boardPosition")
                .param("sortDirection", "ASC"), null, 200);

        JsonNode assignment = submit(post("/api/v1/projects/{id}/assignments", projectId),
                ProjectAssignmentCreateRequestDto.builder()
                        .employeeId(memberEmployeeId)
                        .projectRoleId(idOf(developerRole))
                        .startDate(LocalDate.of(2020, 1, 1))
                        .allocationPercent(new BigDecimal("50.00"))
                        .build(),
                201);
        submit(get("/api/v1/projects/{id}/assignments", projectId)
                .param("projectId", projectId.toString())
                .param("employeeId", memberEmployeeId.toString())
                .param("projectRoleId", idOf(developerRole).toString())
                .param("startDateFrom", "2020-01-01")
                .param("startDateTo", "2020-12-31")
                .param("endDateFrom", "2020-06-01")
                .param("endDateTo", "2020-06-30")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "startDate")
                .param("sortDirection", "DESC"), null, 200);
        submit(patch("/api/v1/assignments/{id}", idOf(assignment)),
                ProjectAssignmentUpdateRequestDto.builder()
                        .allocationPercent(new BigDecimal("80.00"))
                        .build(),
                200);
        submit(post("/api/v1/assignments/{id}/end", idOf(assignment)),
                ProjectAssignmentEndRequestDto.builder().comment("Rolled off the project").build(),
                200);

        JsonNode milestone = submit(post("/api/v1/projects/{id}/milestones", projectId),
                ProjectMilestoneCreateRequestDto.builder()
                        .title("Kickoff")
                        .description("Project kickoff")
                        .dueDate(LocalDate.of(2026, 6, 15))
                        .status("OPEN")
                        .build(),
                201);
        JsonNode milestones = submit(get("/api/v1/projects/{id}/milestones", projectId)
                .param("projectId", projectId.toString())
                .param("status", "OPEN")
                .param("dueDateFrom", "2026-06-01")
                .param("dueDateTo", "2026-06-30")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "dueDate")
                .param("sortDirection", "ASC"), null, 200);
        assertThat(milestones.get("content")).isNotEmpty();
        submit(patch("/api/v1/milestones/{id}", idOf(milestone)),
                ProjectMilestoneUpdateRequestDto.builder().title("Kickoff complete").status("DONE").build(),
                200);
        submit(delete("/api/v1/milestones/{id}", idOf(milestone)), null, 204);

        JsonNode link = submit(post("/api/v1/projects/{id}/links", projectId),
                ProjectLinkCreateRequestDto.builder()
                        .label("Wiki")
                        .url("https://example.com/alpha")
                        .build(),
                201);
        JsonNode links = submit(get("/api/v1/projects/{id}/links", projectId), null, 200);
        assertThat(links).isNotEmpty();
        submit(patch("/api/v1/project-links/{id}", idOf(link)),
                ProjectLinkUpdateRequestDto.builder()
                        .label("Handbook")
                        .url("https://example.com/alpha/handbook")
                        .build(),
                200);
        submit(delete("/api/v1/project-links/{id}", idOf(link)), null, 204);

        submit(patch("/api/v1/boards/{id}", idOf(board)),
                ProjectBoardUpdateRequestDto.builder().name("Delivery board").build(),
                200);
        submit(patch("/api/v1/board-columns/{id}", idOf(extra)),
                BoardColumnUpdateRequestDto.builder().name("Spare").build(),
                200);
        submit(delete("/api/v1/board-columns/{id}", idOf(extra)), null, 204);
    }

    private BoardColumnCreateRequestDto column(
            String code,
            String name,
            int position,
            boolean initial,
            boolean terminal
    ) {
        return BoardColumnCreateRequestDto.builder()
                .code(code)
                .name(name)
                .position(position)
                .isInitial(initial)
                .isTerminal(terminal)
                .build();
    }

    private ProjectRoleCreateRequestDto role(String code, String name) {
        return ProjectRoleCreateRequestDto.builder()
                .code(code)
                .name(name)
                .description(name)
                .active(true)
                .build();
    }

    private <B extends AbstractMockHttpServletRequestBuilder<B>> JsonNode submit(
            B builder,
            Object body,
            int expectedStatus
    ) throws Exception {
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(toJson(body));
        }

        MvcResult result = mockMvc.perform(asAdmin(builder))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        String content = result.getResponse().getContentAsString();

        if (content == null || content.isBlank()) {
            return null;
        }

        return objectMapper.readTree(content);
    }

    private UUID idOf(JsonNode node) {
        return UUID.fromString(node.get("id").asString());
    }

    private long versionOf(JsonNode node) {
        return Long.parseLong(node.get("version").asString());
    }
}
