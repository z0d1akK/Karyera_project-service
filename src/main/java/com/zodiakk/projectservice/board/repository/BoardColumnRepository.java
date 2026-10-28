package com.zodiakk.projectservice.board.repository;

import com.zodiakk.projectservice.board.entity.BoardColumn;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID>, JpaSpecificationExecutor<BoardColumn> {

    List<BoardColumn> findAllByBoardIdOrderByPositionAsc(UUID boardId);

    boolean existsByBoardIdAndCodeIgnoreCase(UUID boardId, String code);

    boolean existsByBoardIdAndCodeIgnoreCaseAndIdNot(UUID boardId, String code, UUID id);

    boolean existsByBoardIdAndPosition(UUID boardId, Integer position);

    Optional<BoardColumn> findFirstByBoardIdAndIsInitialTrue(UUID boardId);

}
