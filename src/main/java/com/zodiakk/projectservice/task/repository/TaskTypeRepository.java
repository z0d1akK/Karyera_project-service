package com.zodiakk.projectservice.task.repository;

import com.zodiakk.projectservice.task.entity.TaskType;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface TaskTypeRepository extends JpaRepository<TaskType, UUID>, JpaSpecificationExecutor<TaskType> {

    Optional<TaskType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<TaskType> findAllByActiveTrueOrderByNameAsc();

}
