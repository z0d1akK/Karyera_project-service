package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.task.entity.TaskComment;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID>, JpaSpecificationExecutor<TaskComment> {

    List<TaskComment> findAllByTaskIdOrderByCreatedAtAsc(UUID taskId);

}
