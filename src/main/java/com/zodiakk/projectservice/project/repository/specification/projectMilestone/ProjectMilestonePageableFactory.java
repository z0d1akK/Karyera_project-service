package com.zodiakk.projectservice.project.repository.specification.projectMilestone;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class ProjectMilestonePageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("title", "title"),
            Map.entry("status", "status"),
            Map.entry("dueDate", "dueDate"),
            Map.entry("completedDate", "completedDate")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "dueDate"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
