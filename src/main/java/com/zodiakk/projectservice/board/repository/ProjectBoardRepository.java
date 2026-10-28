package com.zodiakk.projectservice.board.repository;

import com.zodiakk.projectservice.board.entity.ProjectBoard;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ProjectBoardRepository extends JpaRepository<ProjectBoard, UUID>, JpaSpecificationExecutor<ProjectBoard> {

    List<ProjectBoard> findAllByProjectIdOrderByCreatedAtAsc(UUID projectId);

    Optional<ProjectBoard> findFirstByProjectIdOrderByCreatedAtAsc(UUID projectId);

    boolean existsByProjectId(UUID projectId);

}
