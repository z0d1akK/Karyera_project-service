package com.zodiakk.projectservice.task.controller;

import com.zodiakk.projectservice.common.dto.response.ApiErrorResponse;
import com.zodiakk.projectservice.task.service.TaskRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/v1/task-relations")
@RequiredArgsConstructor
@Tag(name = "Task relations", description = "Operations on task relations")
public class TaskRelationController {

    private final TaskRelationService taskRelationService;

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task relation", description = "Deletes an existing task relation")
    @ApiResponse(responseCode = "204", description = "Relation successfully deleted")
    @ApiResponse(responseCode = "404", description = "Relation not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Relation identifier", required = true) @PathVariable UUID id
    ) {
        taskRelationService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
