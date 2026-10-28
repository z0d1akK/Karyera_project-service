package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.task.entity.TaskSkill;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TaskSkillRepository extends JpaRepository<TaskSkill, UUID>, JpaSpecificationExecutor<TaskSkill> {

    List<TaskSkill> findAllByTaskId(UUID taskId);

    List<TaskSkill> findAllBySkillId(UUID skillId);

    boolean existsByTaskIdAndSkillId(UUID taskId, UUID skillId);

}
