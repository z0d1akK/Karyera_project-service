package com.zodiakk.projectservice.project.repository;

import com.zodiakk.projectservice.project.entity.ProjectLink;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ProjectLinkRepository extends JpaRepository<ProjectLink, UUID>, JpaSpecificationExecutor<ProjectLink> {

    List<ProjectLink> findAllByProjectIdOrderByCreatedAtAsc(UUID projectId);

}
