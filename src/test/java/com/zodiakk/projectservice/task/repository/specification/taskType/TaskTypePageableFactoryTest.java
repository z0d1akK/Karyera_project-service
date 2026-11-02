package com.zodiakk.projectservice.task.repository.specification.taskType;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaskTypePageableFactory")
class TaskTypePageableFactoryTest {

    @Test
    @DisplayName("falls back for a blank sort and honors ASC and DESC")
    void createResolvesSort() {
        assertSort(null, null, "name", Sort.Direction.ASC);
        assertSort("  ", SortDirection.ASC, "name", Sort.Direction.ASC);
        assertSort("not-a-column", SortDirection.DESC, "name", Sort.Direction.DESC);
        assertSort("code", SortDirection.ASC, "code", Sort.Direction.ASC);
        assertSort("code", SortDirection.DESC, "code", Sort.Direction.DESC);
    }

    private static void assertSort(
            String sortBy,
            SortDirection sortDirection,
            String expectedField,
            Sort.Direction expectedDirection
    ) {
        Pageable pageable = TaskTypePageableFactory.create(PageRequestDto.builder()
                .page(0)
                .size(20)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build());

        Sort.Order order = pageable.getSort().getOrderFor(expectedField);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(expectedDirection);
    }
}
