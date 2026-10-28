package com.zodiakk.projectservice.task.repository.specification.taskType;

import com.zodiakk.projectservice.task.dto.request.filter.TaskTypeFilterDto;
import com.zodiakk.projectservice.task.entity.TaskType;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class TaskTypeSpecification {

    public static Specification<TaskType> filter(TaskTypeFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getCode() != null && !filter.getCode().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("code")),
                        "%" + filter.getCode().trim().toLowerCase() + "%"
                ));
            }

            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.getName().trim().toLowerCase() + "%"
                ));
            }

            if (filter.getActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), filter.getActive()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
