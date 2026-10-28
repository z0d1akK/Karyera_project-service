package com.zodiakk.projectservice.project.entity;

import com.zodiakk.projectservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "project_milestones")
@NoArgsConstructor
@SuperBuilder
public class ProjectMilestone extends IdentifiedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

}
