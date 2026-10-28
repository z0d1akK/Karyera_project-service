package com.zodiakk.projectservice.project.repository.specification.projectAssignment;

import com.zodiakk.projectservice.project.dto.request.filter.ProjectAssignmentFilterDto;
import com.zodiakk.projectservice.project.entity.ProjectAssignment;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ProjectAssignmentSpecification {

    public static Specification<ProjectAssignment> filter(ProjectAssignmentFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getProjectId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectId"), filter.getProjectId()));
            }

            if (filter.getEmployeeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("employeeId"), filter.getEmployeeId()));
            }

            if (filter.getProjectRoleId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectRoleId"), filter.getProjectRoleId()));
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
