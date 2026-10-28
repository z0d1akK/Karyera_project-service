package com.zodiakk.projectservice.project.repository;

import com.zodiakk.projectservice.project.entity.ProjectRole;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, UUID>, JpaSpecificationExecutor<ProjectRole> {

    Optional<ProjectRole> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<ProjectRole> findAllByActiveTrueOrderByNameAsc();

}
