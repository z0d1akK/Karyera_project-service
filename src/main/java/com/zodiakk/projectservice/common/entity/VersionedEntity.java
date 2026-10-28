package com.zodiakk.projectservice.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public abstract class VersionedEntity extends BaseEntity {

    @Version
    @Column(nullable = false)
    private Long version;
}
