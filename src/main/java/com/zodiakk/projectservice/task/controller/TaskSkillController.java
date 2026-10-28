package com.zodiakk.projectservice.task.controller;

import com.zodiakk.projectservice.task.service.TaskSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/task-skills")
@RequiredArgsConstructor
@Tag(name = "Task skills", description = "Operations on task skills")
public class TaskSkillController {

    private final TaskSkillService taskSkillService;

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task skill", description = "Removes a required skill from the task")
    @ApiResponse(responseCode = "204", description = "Task skill successfully deleted")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Task skill identifier", required = true) @PathVariable UUID id
    ) {
        taskSkillService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
