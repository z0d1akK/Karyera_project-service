package com.zodiakk.projectservice.board.entity;

import com.zodiakk.projectservice.common.entity.IdentifiedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "board_columns")
@NoArgsConstructor
@SuperBuilder
public class BoardColumn extends IdentifiedEntity {

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "is_initial", nullable = false)
    private Boolean isInitial;

    @Column(name = "is_terminal", nullable = false)
    private Boolean isTerminal;

}
