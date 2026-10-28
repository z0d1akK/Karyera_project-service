package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.common.enums.*;
import com.zodiakk.projectservice.task.entity.TaskRelation;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TaskRelationRepository extends JpaRepository<TaskRelation, UUID>, JpaSpecificationExecutor<TaskRelation> {

    List<TaskRelation> findAllBySourceTaskId(UUID sourceTaskId);

    List<TaskRelation> findAllBySourceTaskIdAndRelationType(UUID sourceTaskId, TaskRelationType relationType);

    List<TaskRelation> findAllByTargetTaskIdInAndRelationType(
            Collection<UUID> targetTaskIds,
            TaskRelationType relationType
    );

    boolean existsBySourceTaskIdAndTargetTaskIdAndRelationType(
            UUID sourceTaskId,
            UUID targetTaskId,
            TaskRelationType relationType
    );

}
