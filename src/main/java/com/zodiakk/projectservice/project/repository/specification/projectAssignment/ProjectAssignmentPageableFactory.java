package com.zodiakk.projectservice.project.repository.specification.projectAssignment;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class ProjectAssignmentPageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("startDate", "startDate"),
            Map.entry("endDate", "endDate"),
            Map.entry("allocationPercent", "allocationPercent"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("updatedAt", "updatedAt"),
            Map.entry("version", "version")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = sortField(pageRequest.getSortBy(), "startDate");

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }

    private static String sortField(String requested, String fallback) {
        if (requested == null) {
            return fallback;
        }

        return ALLOWED_SORT_FIELDS.getOrDefault(requested, fallback);
    }
}
