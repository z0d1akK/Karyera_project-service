package com.zodiakk.projectservice.project.repository;

import com.zodiakk.projectservice.project.entity.ProjectAssignment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.*;
import java.util.*;

@Repository
public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, UUID>, JpaSpecificationExecutor<ProjectAssignment> {

    long countByProjectId(UUID projectId);

    long countByProjectRoleId(UUID projectRoleId);

    List<ProjectAssignment> findAllByEmployeeId(UUID employeeId);

    boolean existsByProjectIdAndEmployeeIdAndEndDateIsNull(UUID projectId, UUID employeeId);

    boolean existsByProjectIdAndEmployeeId(UUID projectId, UUID employeeId);

    @Query("""
            SELECT a FROM ProjectAssignment a
            WHERE a.employeeId = :employeeId
              AND a.startDate <= :date
              AND (a.endDate IS NULL OR a.endDate >= :date)
            """)
    List<ProjectAssignment> findActiveOnDate(
            @Param("employeeId") UUID employeeId,
            @Param("date") LocalDate date
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM ProjectAssignment a WHERE a.id = :assignmentId")
    Optional<ProjectAssignment> findByIdForUpdate(@Param("assignmentId") UUID assignmentId);

}
