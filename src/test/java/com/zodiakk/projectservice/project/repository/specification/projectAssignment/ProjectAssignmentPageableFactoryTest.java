package com.zodiakk.projectservice.project.repository.specification.projectAssignment;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectAssignmentPageableFactory")
class ProjectAssignmentPageableFactoryTest {

    @Test
    @DisplayName("falls back for a blank sort and honors ASC and DESC")
    void createResolvesSort() {
        assertSort(null, null, "startDate", Sort.Direction.ASC);
        assertSort("  ", SortDirection.ASC, "startDate", Sort.Direction.ASC);
        assertSort("not-a-column", SortDirection.DESC, "startDate", Sort.Direction.DESC);
        assertSort("endDate", SortDirection.ASC, "endDate", Sort.Direction.ASC);
        assertSort("endDate", SortDirection.DESC, "endDate", Sort.Direction.DESC);
    }

    private static void assertSort(
            String sortBy,
            SortDirection sortDirection,
            String expectedField,
            Sort.Direction expectedDirection
    ) {
        Pageable pageable = ProjectAssignmentPageableFactory.create(PageRequestDto.builder()
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
