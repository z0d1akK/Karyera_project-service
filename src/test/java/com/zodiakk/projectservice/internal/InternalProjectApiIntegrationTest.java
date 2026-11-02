package com.zodiakk.projectservice.internal;

import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskAssigneesUpdateRequestDto;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal project API")
class InternalProjectApiIntegrationTest extends AbstractIntegrationTest {

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
    @DisplayName("resolves work eligibility, history, statistics, membership and completion evidence")
    void coversInternalReads() throws Exception {
        JsonNode role = submit(post("/api/v1/project-roles"),
                ProjectRoleCreateRequestDto.builder().code("DEV").name("Developer").active(true).build(),
                201);
        JsonNode project = submit(post("/api/v1/projects"), ProjectCreateRequestDto.builder()
                .code("GAMMA")
                .name("Gamma")
                .description("Gamma delivery")
                .startDate(LocalDate.now().minusMonths(1))
                .projectManagerId(adminEmployeeId)
                .build(), 201);
        UUID projectId = idOf(project);
        submit(patch("/api/v1/projects/{id}/status", projectId),
                ProjectStatusUpdateRequestDto.builder()
                        .status(ProjectStatus.ACTIVE)
                        .expectedVersion(versionOf(project))
                        .build(),
                200);

        submit(post("/api/v1/projects/{id}/board", projectId),
                ProjectBoardUpdateRequestDto.builder().name("Gamma board").build(),
                201);
        UUID doneColumnId = idOf(submit(post("/api/v1/projects/{id}/board/columns", projectId),
                BoardColumnCreateRequestDto.builder()
                        .code("DONE")
                        .name("Done")
                        .position(0)
                        .isInitial(true)
                        .isTerminal(true)
                        .build(),
                201));
        JsonNode taskType = submit(post("/api/v1/task-types"),
                TaskTypeCreateRequestDto.builder().code("TASK").name("Task").active(true).build(),
                201);
        JsonNode task = submit(post("/api/v1/projects/{id}/tasks", projectId),
                ProjectTaskCreateRequestDto.builder()
                        .title("Ship the release")
                        .description("Release work")
                        .taskTypeId(idOf(taskType))
                        .columnId(doneColumnId)
                        .priority(TaskPriority.HIGH)
                        .complexity(TaskComplexity.MEDIUM)
                        .estimatedMinutes(30)
                        .dueDate(LocalDate.now())
                        .build(),
                201);
        JsonNode assignment = submit(post("/api/v1/projects/{id}/assignments", projectId),
                ProjectAssignmentCreateRequestDto.builder()
                        .employeeId(memberEmployeeId)
                        .projectRoleId(idOf(role))
                        .startDate(LocalDate.now().minusDays(10))
                        .allocationPercent(new BigDecimal("100"))
                        .build(),
                201);
        submit(put("/api/v1/tasks/{id}/assignees", idOf(task)),
                TaskAssigneesUpdateRequestDto.builder()
                        .assigneeIds(List.of(idOf(assignment)))
                        .expectedVersion(versionOf(task))
                        .build(),
                200);

        LocalDate today = LocalDate.now();

        mockMvc.perform(asInternal(get("/internal/v1/projects/{projectId}/work-eligibility", projectId)
                        .param("employeeId", memberEmployeeId.toString())
                        .param("taskId", idOf(task).toString())
                        .param("workDate", today.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.projectActive").value(true))
                .andExpect(jsonPath("$.assignmentActiveOnDate").value(true));

        mockMvc.perform(asInternal(get("/internal/v1/employees/{employeeId}/project-history", memberEmployeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(memberEmployeeId.toString()))
                .andExpect(jsonPath("$.projects.length()").value(1));

        mockMvc.perform(asInternal(get("/internal/v1/employees/{employeeId}/task-statistics", memberEmployeeId)
                        .param("periodStart", today.minusDays(1).toString())
                        .param("periodEnd", today.plusDays(1).toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(memberEmployeeId.toString()))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.projectsParticipated").value(1));

        mockMvc.perform(asInternal(get(
                        "/internal/v1/projects/{projectId}/members/{employeeId}/eligibility",
                        projectId,
                        memberEmployeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.assignmentActive").value(true));

        mockMvc.perform(asInternal(get("/internal/v1/tasks/{taskId}/completion-evidence", idOf(task))
                        .param("employeeId", memberEmployeeId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCompleted").value(true))
                .andExpect(jsonPath("$.employeeParticipated").value(true));
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
