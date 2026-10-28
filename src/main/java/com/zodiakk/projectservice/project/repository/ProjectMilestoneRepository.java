package com.zodiakk.projectservice.project.repository;

import com.zodiakk.projectservice.project.entity.ProjectMilestone;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, UUID>, JpaSpecificationExecutor<ProjectMilestone> {

}
