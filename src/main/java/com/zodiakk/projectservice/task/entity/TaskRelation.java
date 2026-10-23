package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.CreatedEntity;
import com.zodiakk.projectservice.common.enums.TaskRelationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "task_relations")
@NoArgsConstructor
@SuperBuilder
public class TaskRelation extends CreatedEntity {

    @Column(name = "source_task_id", nullable = false)
    private UUID sourceTaskId;

    @Column(name = "target_task_id", nullable = false)
    private UUID targetTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    private TaskRelationType relationType;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

}
