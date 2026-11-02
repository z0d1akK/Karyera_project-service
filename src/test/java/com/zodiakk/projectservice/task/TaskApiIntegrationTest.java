package com.zodiakk.projectservice.task;

import com.zodiakk.projectservice.board.dto.request.create.BoardColumnCreateRequestDto;
import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import com.zodiakk.projectservice.common.enums.ProjectStatus;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import com.zodiakk.projectservice.project.dto.request.action.ProjectStatusUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectAssignmentCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskAssigneesUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskBoardPositionUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskCommentCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskParentUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.action.TaskRelationCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.ProjectTaskCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskSkillCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.create.TaskTypeCreateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.ProjectTaskUpdateRequestDto;
import com.zodiakk.projectservice.task.dto.request.update.TaskTypeUpdateRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Task API")
class TaskApiIntegrationTest extends AbstractIntegrationTest {

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
    @DisplayName("covers task commands and task type catalog endpoints")
    void coversTaskApi() throws Exception {
        JsonNode role = submit(post("/api/v1/project-roles"),
                ProjectRoleCreateRequestDto.builder().code("DEV").name("Developer").active(true).build(),
                201);
        JsonNode project = submit(post("/api/v1/projects"), ProjectCreateRequestDto.builder()
                .code("BETA")
                .name("Beta")
                .description("Beta delivery")
                .startDate(LocalDate.of(2026, 1, 15))
                .endDate(LocalDate.of(2026, 12, 15))
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
                ProjectBoardUpdateRequestDto.builder().name("Beta board").build(),
                201);
        UUID todoColumnId = idOf(submit(post("/api/v1/projects/{id}/board/columns", projectId),
                column("TODO", "To do", 0, true, false), 201));
        UUID doingColumnId = idOf(submit(post("/api/v1/projects/{id}/board/columns", projectId),
                column("DOING", "Doing", 1, false, false), 201));

        JsonNode epicType = submit(post("/api/v1/task-types"), taskType("EPIC", "Epic"), 201);
        JsonNode storyType = submit(post("/api/v1/task-types"), taskType("STORY", "Story"), 201);
        JsonNode spareType = submit(post("/api/v1/task-types"), taskType("TEMP", "Temporary"), 201);

        JsonNode taskTypes = submit(get("/api/v1/task-types")
                .param("code", "EPIC")
                .param("name", "Epic")
                .param("active", "true")
                .param("page", "0")
                .param("size", "20")
                .param("sortBy", "code")
                .param("sortDirection", "DESC"), null, 200);
        assertThat(taskTypes.get("content")).isNotEmpty();
        submit(get("/api/v1/task-types/{id}", idOf(epicType)), null, 200);
        submit(patch("/api/v1/task-types/{id}", idOf(storyType)),
                TaskTypeUpdateRequestDto.builder().description("User story").build(),
                200);
        submit(delete("/api/v1/task-types/{id}", idOf(spareType)), null, 204);

        JsonNode assignment = submit(post("/api/v1/projects/{id}/assignments", projectId),
                ProjectAssignmentCreateRequestDto.builder()
                        .employeeId(memberEmployeeId)
                        .projectRoleId(idOf(role))
                        .startDate(LocalDate.of(2020, 1, 1))
                        .allocationPercent(new BigDecimal("100"))
                        .build(),
                201);

        JsonNode epic = submit(post("/api/v1/projects/{id}/tasks", projectId),
                task("Launch epic", idOf(epicType), todoColumnId, null), 201);
        JsonNode story = submit(post("/api/v1/projects/{id}/tasks", projectId),
                task("Write the story", idOf(storyType), todoColumnId, null), 201);
        UUID storyId = idOf(story);

        submit(get("/api/v1/tasks/{id}", storyId), null, 200);
        story = submit(patch("/api/v1/tasks/{id}", storyId),
                ProjectTaskUpdateRequestDto.builder()
                        .title("Write the updated story")
                        .description("Refined scope")
                        .priority(TaskPriority.HIGH)
                        .complexity(TaskComplexity.HIGH)
                        .storyPoints(new BigDecimal("5"))
                        .estimatedMinutes(90)
                        .dueDate(LocalDate.of(2026, 7, 1))
                        .build(),
                200);
        story = submit(patch("/api/v1/tasks/{id}/parent", storyId),
                TaskParentUpdateRequestDto.builder()
                        .parentTaskId(idOf(epic))
                        .expectedVersion(versionOf(story))
                        .build(),
                200);

        JsonNode children = submit(get("/api/v1/tasks/{id}/children", idOf(epic)), null, 200);
        assertThat(children).hasSize(1);

        submit(patch("/api/v1/tasks/{id}/board-position", storyId),
                TaskBoardPositionUpdateRequestDto.builder()
                        .targetColumnId(doingColumnId)
                        .expectedVersion(versionOf(story))
                        .build(),
                200);
        JsonNode currentStory = submit(get("/api/v1/tasks/{id}", storyId), null, 200);

        submit(put("/api/v1/tasks/{id}/assignees", storyId),
                TaskAssigneesUpdateRequestDto.builder()
                        .assigneeIds(List.of(idOf(assignment)))
                        .expectedVersion(versionOf(currentStory))
                        .build(),
                200);
        JsonNode assignees = submit(get("/api/v1/tasks/{id}/assignees", storyId), null, 200);
        assertThat(assignees).isNotEmpty();

        submit(post("/api/v1/tasks/{id}/comments", storyId),
                TaskCommentCreateRequestDto.builder()
                        .authorEmployeeId(adminEmployeeId)
                        .content("Shipping this week")
                        .build(),
                201);
        JsonNode comments = submit(get("/api/v1/tasks/{id}/comments", storyId), null, 200);
        assertThat(comments).isNotEmpty();

        JsonNode activities = submit(get("/api/v1/tasks/{id}/activities", storyId), null, 200);
        assertThat(activities).isNotEmpty();

        JsonNode relation = submit(post("/api/v1/tasks/{id}/relations", storyId),
                TaskRelationCreateRequestDto.builder()
                        .targetTaskId(idOf(epic))
                        .relationType(TaskRelationType.RELATES_TO)
                        .build(),
                201);
        JsonNode relations = submit(get("/api/v1/tasks/{id}/relations", storyId), null, 200);
        assertThat(relations).isNotEmpty();
        submit(delete("/api/v1/task-relations/{id}", idOf(relation)), null, 204);

        JsonNode skill = submit(post("/api/v1/tasks/{id}/skills", storyId),
                TaskSkillCreateRequestDto.builder().skillId(UUID.randomUUID()).build(),
                201);
        JsonNode skills = submit(get("/api/v1/tasks/{id}/skills", storyId), null, 200);
        assertThat(skills).isNotEmpty();
        submit(delete("/api/v1/task-skills/{id}", idOf(skill)), null, 204);

        submit(delete("/api/v1/tasks/{id}", storyId), null, 204);
    }

    private ProjectTaskCreateRequestDto task(String title, UUID taskTypeId, UUID columnId, UUID parentTaskId) {
        return ProjectTaskCreateRequestDto.builder()
                .title(title)
                .description(title)
                .taskTypeId(taskTypeId)
                .columnId(columnId)
                .parentTaskId(parentTaskId)
                .priority(TaskPriority.MEDIUM)
                .complexity(TaskComplexity.LOW)
                .storyPoints(new BigDecimal("2"))
                .estimatedMinutes(60)
                .dueDate(LocalDate.of(2026, 6, 1))
                .build();
    }

    private TaskTypeCreateRequestDto taskType(String code, String name) {
        return TaskTypeCreateRequestDto.builder()
                .code(code)
                .name(name)
                .description(name)
                .active(true)
                .build();
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
