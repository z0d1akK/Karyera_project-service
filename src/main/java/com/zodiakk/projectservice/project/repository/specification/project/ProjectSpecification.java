package com.zodiakk.projectservice.project.repository.specification.project;

import com.zodiakk.projectservice.project.dto.request.filter.ProjectFilterDto;
import com.zodiakk.projectservice.project.entity.Project;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ProjectSpecification {

    public static Specification<Project> visibleTo(UUID employeeId) {
        return (root, query, criteriaBuilder) -> {
            Subquery<UUID> assignmentSubquery = query.subquery(UUID.class);
            Root<ProjectAssignment> assignment = assignmentSubquery.from(ProjectAssignment.class);

            assignmentSubquery.select(assignment.get("projectId"))
                    .where(criteriaBuilder.equal(assignment.get("employeeId"), employeeId));

            return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("projectManagerId"), employeeId),
                    criteriaBuilder.equal(root.get("deliveryManagerId"), employeeId),
                    root.get("id").in(assignmentSubquery)
            );
        };
    }

    public static Specification<Project> filter(ProjectFilterDto filter) {
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

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getProjectManagerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectManagerId"), filter.getProjectManagerId()));
            }

            if (filter.getDeliveryManagerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("deliveryManagerId"), filter.getDeliveryManagerId()));
            }

            if (filter.getStartDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDateFrom()));
            }

            if (filter.getStartDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), filter.getStartDateTo()));
            }

            if (filter.getEndDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), filter.getEndDateFrom()));
            }

            if (filter.getEndDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), filter.getEndDateTo()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
