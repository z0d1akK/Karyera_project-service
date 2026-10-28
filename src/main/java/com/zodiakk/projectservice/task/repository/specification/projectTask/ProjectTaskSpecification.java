package com.zodiakk.projectservice.task.repository.specification.projectTask;

import com.zodiakk.projectservice.task.dto.request.filter.ProjectTaskFilterDto;
import com.zodiakk.projectservice.task.entity.ProjectTask;

import jakarta.persistence.criteria.*;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

@UtilityClass
public class ProjectTaskSpecification {

    public static Specification<ProjectTask> filter(ProjectTaskFilterDto filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            if (filter.getProjectId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("projectId"), filter.getProjectId()));
            }

            if (filter.getColumnId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("columnId"), filter.getColumnId()));
            }

            if (filter.getTaskTypeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("taskTypeId"), filter.getTaskTypeId()));
            }

            if (filter.getParentTaskId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("parentTaskId"), filter.getParentTaskId()));
            }

            if (filter.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), filter.getPriority()));
            }

            if (filter.getComplexity() != null) {
                predicates.add(criteriaBuilder.equal(root.get("complexity"), filter.getComplexity()));
            }

            if (filter.getTaskNumber() != null) {
                predicates.add(criteriaBuilder.equal(root.get("taskNumber"), filter.getTaskNumber()));
            }

            if (filter.getArchived() != null) {
                predicates.add(Boolean.TRUE.equals(filter.getArchived())
                        ? criteriaBuilder.isNotNull(root.get("archivedAt"))
                        : criteriaBuilder.isNull(root.get("archivedAt")));
            }

            if (filter.getDueDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), filter.getDueDateFrom()));
            }

            if (filter.getDueDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), filter.getDueDateTo()));
            }

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
                ));
            }

            if (filter.getAssigneeAssignmentId() != null) {
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<com.zodiakk.projectservice.task.entity.TaskAssignee> assignee = subquery.from(com.zodiakk.projectservice.task.entity.TaskAssignee.class);
                subquery.select(assignee.get("taskId"))
                        .where(
                                criteriaBuilder.equal(assignee.get("assignmentId"), filter.getAssigneeAssignmentId()),
                                criteriaBuilder.isNull(assignee.get("unassignedAt"))
                        );
                predicates.add(root.get("id").in(subquery));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
