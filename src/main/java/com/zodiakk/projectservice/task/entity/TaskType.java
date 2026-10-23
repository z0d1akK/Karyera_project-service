package com.zodiakk.projectservice.task.entity;

import com.zodiakk.projectservice.common.entity.IdentifiedEntity;
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
@Table(name = "task_types")
@NoArgsConstructor
@SuperBuilder
public class TaskType extends IdentifiedEntity {

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

}
