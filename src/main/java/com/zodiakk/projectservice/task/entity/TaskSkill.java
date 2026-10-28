package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.CreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "task_skills")
@NoArgsConstructor
@SuperBuilder
public class TaskSkill extends CreatedEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

}
