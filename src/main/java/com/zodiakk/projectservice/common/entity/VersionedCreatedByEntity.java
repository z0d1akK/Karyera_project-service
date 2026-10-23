package com.zodiakk.projectservice.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class VersionedCreatedByEntity extends BaseEntity {

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
