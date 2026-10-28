package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "task_assignees")
@NoArgsConstructor
@SuperBuilder
public class TaskAssignee extends IdentifiedEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "unassigned_at")
    private OffsetDateTime unassignedAt;

}
