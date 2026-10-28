package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.task.entity.TaskActivity;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID>, JpaSpecificationExecutor<TaskActivity> {

    List<TaskActivity> findAllByTaskIdOrderByCreatedAtDesc(UUID taskId);

    List<TaskActivity> findAllByActorEmployeeId(UUID actorEmployeeId);

}
