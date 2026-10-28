package com.zodiakk.projectservice.project.entity;

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
@Table(name = "project_links")
@NoArgsConstructor
@SuperBuilder
public class ProjectLink extends CreatedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

}
