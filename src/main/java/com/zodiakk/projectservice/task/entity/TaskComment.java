package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.BaseEntity;
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
@Table(name = "task_comments")
@NoArgsConstructor
@SuperBuilder
public class TaskComment extends BaseEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "author_employee_id", nullable = false)
    private UUID authorEmployeeId;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

}
