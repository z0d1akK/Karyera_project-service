package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.VersionedEntity;
import com.zodiakk.projectservice.common.enums.TaskComplexity;
import com.zodiakk.projectservice.common.enums.TaskPriority;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "project_tasks")
@NoArgsConstructor
@SuperBuilder
public class ProjectTask extends VersionedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_number", nullable = false)
    private Long taskNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "task_type_id", nullable = false)
    private UUID taskTypeId;

    @Column(name = "column_id", nullable = false)
    private UUID columnId;

    @Column(name = "parent_task_id")
    private UUID parentTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "complexity", length = 20)
    private TaskComplexity complexity;

    @Column(name = "story_points", precision = 5, scale = 2)
    private BigDecimal storyPoints;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "board_position", nullable = false, precision = 30, scale = 15)
    private BigDecimal boardPosition;

    @Column(name = "reporter_employee_id", nullable = false)
    private UUID reporterEmployeeId;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

}
