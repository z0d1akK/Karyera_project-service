package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.CreatedEntity;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
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
@Table(name = "task_activities")
@NoArgsConstructor
@SuperBuilder
public class TaskActivity extends CreatedEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "actor_employee_id", nullable = false)
    private UUID actorEmployeeId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "changes", columnDefinition = "jsonb")
    private String changes;

}
