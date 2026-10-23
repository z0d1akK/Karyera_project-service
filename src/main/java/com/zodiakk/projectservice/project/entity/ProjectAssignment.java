package com.zodiakk.projectservice.project.entity;

import com.zodiakk.projectservice.common.entity.VersionedEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "project_assignments")
@NoArgsConstructor
@SuperBuilder
public class ProjectAssignment extends VersionedEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "project_role_id", nullable = false)
    private UUID projectRoleId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "allocation_percent", precision = 5, scale = 2)
    private BigDecimal allocationPercent;

}
