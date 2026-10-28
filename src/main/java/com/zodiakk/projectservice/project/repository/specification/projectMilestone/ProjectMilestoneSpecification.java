package com.zodiakk.projectservice.project.repository.specification.projectMilestone;

import com.zodiakk.projectservice.project.dto.request.filter.ProjectMilestoneFilterDto;
import com.zodiakk.projectservice.project.entity.ProjectMilestone;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ProjectMilestoneSpecification {

    public static Specification<ProjectMilestone> filter(ProjectMilestoneFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getProjectId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectId"), filter.getProjectId()));
            }

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("status")),
                        "%" + filter.getStatus().trim().toLowerCase() + "%"
                ));
            }

            if (filter.getDueDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }

            if (filter.getDueDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
