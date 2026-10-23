package com.zodiakk.projectservice.board.entity;

import com.zodiakk.projectservice.common.entity.CreatedEntity;
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
@Table(name = "project_boards")
@NoArgsConstructor
@SuperBuilder
public class ProjectBoard extends CreatedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

}
