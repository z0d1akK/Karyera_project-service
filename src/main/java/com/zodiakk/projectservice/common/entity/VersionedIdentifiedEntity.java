package com.zodiakk.projectservice.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class VersionedIdentifiedEntity extends IdentifiedEntity {

    @Version
    @Column(nullable = false)
    private Long version;
}
