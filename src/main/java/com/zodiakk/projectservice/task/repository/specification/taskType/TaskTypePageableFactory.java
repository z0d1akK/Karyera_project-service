package com.zodiakk.projectservice.task.repository.specification.taskType;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

@UtilityClass
public class TaskTypePageableFactory {

    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.ofEntries(
            Map.entry("code", "code"),
            Map.entry("name", "name"),
            Map.entry("active", "active")
    );

    public static Pageable create(PageRequestDto pageRequest) {
        String sortField = ALLOWED_SORT_FIELDS.getOrDefault(
                pageRequest.getSortBy(),
                "name"
        );

        Sort.Direction direction = pageRequest.getSortDirection() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by(direction, sortField));
    }
}
