package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.task.entity.TaskAssignee;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID>, JpaSpecificationExecutor<TaskAssignee> {

    List<TaskAssignee> findAllByTaskIdAndUnassignedAtIsNull(UUID taskId);

    List<TaskAssignee> findAllByTaskIdInAndUnassignedAtIsNull(Collection<UUID> taskIds);

}
