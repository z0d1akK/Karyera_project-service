package com.zodiakk.projectservice.project.repository.specification.projectRole;

import com.zodiakk.projectservice.project.dto.request.filter.ProjectRoleFilterDto;
import com.zodiakk.projectservice.project.entity.ProjectRole;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ProjectRoleSpecification {

    public static Specification<ProjectRole> filter(ProjectRoleFilterDto filter) {
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
