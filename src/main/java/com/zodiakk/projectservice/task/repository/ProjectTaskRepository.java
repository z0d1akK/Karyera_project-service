package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.task.entity.ProjectTask;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID>, JpaSpecificationExecutor<ProjectTask> {

    List<ProjectTask> findAllByProjectIdAndArchivedAtIsNullOrderByBoardPositionAsc(UUID projectId);

    List<ProjectTask> findAllByColumnIdOrderByBoardPositionAsc(UUID columnId);

    List<ProjectTask> findAllByParentTaskId(UUID parentTaskId);

    long countByParentTaskId(UUID parentTaskId);

    long countByParentTaskIdAndCompletedAtIsNotNull(UUID parentTaskId);

    long countByProjectIdAndArchivedAtIsNull(UUID projectId);

    long countByColumnIdAndArchivedAtIsNull(UUID columnId);

    long countByTaskTypeId(UUID taskTypeId);

    @Query("""
            SELECT t FROM ProjectTask t
            WHERE t.completedAt IS NOT NULL
              AND t.completedAt >= :from
              AND t.completedAt < :to
              AND EXISTS (
                  SELECT 1 FROM TaskAssignee a
                  WHERE a.taskId = t.id
                    AND a.assignmentId IN :assignmentIds
              )
            """)
    List<ProjectTask> findCompletedByAssignmentIds(
            @Param("assignmentIds") List<UUID> assignmentIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("SELECT COALESCE(MAX(t.taskNumber), 0) FROM ProjectTask t WHERE t.projectId = :projectId")
    Long findMaxTaskNumber(@Param("projectId") UUID projectId);

    @Query("SELECT COALESCE(MAX(t.boardPosition), 0) FROM ProjectTask t WHERE t.columnId = :columnId")
    BigDecimal findMaxBoardPosition(@Param("columnId") UUID columnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ProjectTask t WHERE t.id = :taskId")
    Optional<ProjectTask> findByIdForUpdate(@Param("taskId") UUID taskId);

}
