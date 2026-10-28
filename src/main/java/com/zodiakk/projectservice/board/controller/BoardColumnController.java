package com.zodiakk.projectservice.board.controller;

import com.zodiakk.projectservice.board.dto.request.update.BoardColumnUpdateRequestDto;
import com.zodiakk.projectservice.board.dto.response.BoardColumnResponseDto;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/board-columns")
@RequiredArgsConstructor
@Tag(name = "Board columns", description = "Operations on board columns")
public class BoardColumnController {

    private final BoardService boardService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update board column", description = "Updates an existing board column")
    @ApiResponse(responseCode = "200", description = "Column successfully updated",
            content = @Content(schema = @Schema(implementation = BoardColumnResponseDto.class)))
    public ResponseEntity<BoardColumnResponseDto> update(
            @Parameter(description = "Column identifier", required = true) @PathVariable UUID id,
            @Valid @RequestBody BoardColumnUpdateRequestDto request
    ) {
        return ResponseEntity.ok(boardService.updateColumn(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete board column", description = "Deletes a board column that has no tasks")
    @ApiResponse(responseCode = "204", description = "Column successfully deleted")
    @ApiResponse(responseCode = "409", description = "Column contains tasks",
            content = @Content(schema = @Schema(implementation = com.zodiakk.projectservice.common.dto.response.ApiErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Column identifier", required = true) @PathVariable UUID id
    ) {
        boardService.deleteColumn(id);

        return ResponseEntity.noContent().build();
    }
}
