package com.zodiakk.projectservice.board.controller;

import com.zodiakk.projectservice.board.dto.request.update.ProjectBoardUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.ProjectBoardResponseDto;
import com.zodiakk.projectservice.board.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/boards")
@RequiredArgsConstructor
@Tag(name = "Boards", description = "Operations on project boards")
public class ProjectBoardController {

    private final BoardService boardService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update board", description = "Updates the project board name")
    @ApiResponse(responseCode = "200", description = "Board successfully updated",
            content = @Content(schema = @Schema(implementation = ProjectBoardResponseDto.class)))
    public ResponseEntity<ProjectBoardResponseDto> update(
            @Parameter(description = "Board identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProjectBoardUpdateRequestDto request
    ) {
        return ResponseEntity.ok(boardService.update(id, request));
    }
}
