package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;
import com.zodiakk.projectservice.project.dto.request.create.ProjectMilestoneCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectMilestoneFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectMilestoneUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectMilestoneResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectMilestone;
import com.zodiakk.projectservice.project.exception.milestone.ProjectMilestoneNotFoundException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectMilestoneMapper;
import com.zodiakk.projectservice.project.repository.ProjectMilestoneRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import com.zodiakk.projectservice.security.access.ProjectAccessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMilestoneServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID MILESTONE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String TITLE = "Kickoff";

    private static final LocalDate DUE_DATE = LocalDate.of(2026, 3, 1);

    @Mock
    private ProjectMilestoneRepository projectMilestoneRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMilestoneMapper projectMilestoneMapper;

    @Mock
    private ProjectAccessService projectAccessService;

    @InjectMocks
    private ProjectMilestoneServiceImpl projectMilestoneService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("returns an empty page when the caller cannot view the project")
        void shouldReturnEmptyPageWhenProjectIsHidden() {
            ProjectMilestoneFilterDto filter = ProjectMilestoneFilterDto.builder()
                    .projectId(PROJECT_ID)
                    .status("OPEN")
                    .build();

            when(projectAccessService.canViewProject(PROJECT_ID)).thenReturn(false);

            Page<ProjectMilestoneResponseDto> result = projectMilestoneService.findAll(filter, pageRequest());

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verifyNoInteractions(projectMilestoneRepository, projectMilestoneMapper);
        }

        @Test
        @DisplayName("maps a visible project's milestones and preserves paging")
        void shouldMapVisibleMilestones() {
            ProjectMilestoneFilterDto filter = ProjectMilestoneFilterDto.builder()
                    .projectId(PROJECT_ID)
                    .status("OPEN")
                    .dueDateFrom(DUE_DATE)
                    .dueDateTo(DUE_DATE.plusDays(10))
                    .build();
            PageRequestDto pageRequest = pageRequest();
            ProjectMilestone entity = milestone();
            ProjectMilestoneResponseDto expected = response();
            Page<ProjectMilestone> page = new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 6);

            when(projectAccessService.canViewProject(PROJECT_ID)).thenReturn(true);
            when(projectMilestoneRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(projectMilestoneMapper.toResponseDto(entity)).thenReturn(expected);

            Page<ProjectMilestoneResponseDto> result = projectMilestoneService.findAll(filter, pageRequest);

            assertThat(result.getContent()).containsExactly(expected);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectMilestoneRepository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("dueDate")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("dueDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("lists milestones without an access check when no project is requested")
        void shouldSkipAccessCheckWhenProjectIsAbsent() {
            ProjectMilestoneFilterDto filter = ProjectMilestoneFilterDto.builder().status("OPEN").build();

            when(projectMilestoneRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<ProjectMilestoneResponseDto> result = projectMilestoneService.findAll(filter, pageRequest());

            assertThat(result.getContent()).isEmpty();
            verify(projectAccessService, never()).canViewProject(any());
            verify(projectMilestoneMapper, never()).toResponseDto(any());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves a milestone and stamps the path project id")
        void shouldCreateMilestone() {
            ProjectMilestoneCreateRequestDto request = ProjectMilestoneCreateRequestDto.builder()
                    .projectId(OTHER_PROJECT_ID)
                    .title(TITLE)
                    .description("Start")
                    .dueDate(DUE_DATE)
                    .status("OPEN")
                    .build();
            ProjectMilestone entity = milestone();
            ProjectMilestoneResponseDto expected = response();

            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(projectMilestoneMapper.toEntity(request)).thenReturn(entity);
            when(projectMilestoneRepository.save(entity)).thenReturn(entity);
            when(projectMilestoneMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectMilestoneResponseDto result = projectMilestoneService.create(PROJECT_ID, request);

            assertThat(result).isSameAs(expected);
            assertThat(request.getProjectId()).isEqualTo(PROJECT_ID);
            verify(projectMilestoneMapper).toEntity(request);
            verify(projectMilestoneRepository).save(entity);
        }

        @Test
        @DisplayName("throws when the project does not exist")
        void shouldThrowWhenProjectIsMissing() {
            ProjectMilestoneCreateRequestDto request = ProjectMilestoneCreateRequestDto.builder()
                    .projectId(OTHER_PROJECT_ID)
                    .title(TITLE)
                    .status("OPEN")
                    .build();

            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

            assertThatThrownBy(() -> projectMilestoneService.create(PROJECT_ID, request))
                    .isInstanceOf(ProjectNotFoundException.class)
                    .hasMessageContaining(PROJECT_ID.toString());

            assertThat(request.getProjectId()).isEqualTo(PROJECT_ID);
            verifyNoInteractions(projectMilestoneMapper);
            verify(projectMilestoneRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates an existing milestone")
        void shouldUpdateMilestone() {
            ProjectMilestone entity = milestone();
            ProjectMilestoneUpdateRequestDto request = ProjectMilestoneUpdateRequestDto.builder()
                    .title("Launch")
                    .status("DONE")
                    .completedDate(DUE_DATE)
                    .build();
            ProjectMilestoneResponseDto expected = response();

            when(projectMilestoneRepository.findById(MILESTONE_ID)).thenReturn(Optional.of(entity));
            when(projectMilestoneRepository.save(entity)).thenReturn(entity);
            when(projectMilestoneMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectMilestoneResponseDto result = projectMilestoneService.update(MILESTONE_ID, request);

            assertThat(result).isSameAs(expected);
            verify(projectMilestoneMapper).updateEntity(request, entity);
            verify(projectMilestoneRepository).save(entity);
        }

        @Test
        @DisplayName("throws when the milestone does not exist")
        void shouldThrowWhenMissing() {
            ProjectMilestoneUpdateRequestDto request = ProjectMilestoneUpdateRequestDto.builder()
                    .title("Launch")
                    .build();

            when(projectMilestoneRepository.findById(MILESTONE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectMilestoneService.update(MILESTONE_ID, request))
                    .isInstanceOf(ProjectMilestoneNotFoundException.class)
                    .hasMessageContaining(MILESTONE_ID.toString());

            verify(projectMilestoneMapper, never()).updateEntity(any(), any());
            verify(projectMilestoneRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes an existing milestone")
        void shouldDeleteMilestone() {
            ProjectMilestone entity = milestone();

            when(projectMilestoneRepository.findById(MILESTONE_ID)).thenReturn(Optional.of(entity));

            projectMilestoneService.delete(MILESTONE_ID);

            verify(projectMilestoneRepository).delete(entity);
        }

        @Test
        @DisplayName("throws when the milestone does not exist")
        void shouldThrowWhenMissing() {
            when(projectMilestoneRepository.findById(MILESTONE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectMilestoneService.delete(MILESTONE_ID))
                    .isInstanceOf(ProjectMilestoneNotFoundException.class)
                    .hasMessageContaining(MILESTONE_ID.toString());

            verify(projectMilestoneRepository, never()).delete(any(ProjectMilestone.class));
        }
    }

    private static PageRequestDto pageRequest() {
        return PageRequestDto.builder()
                .page(1)
                .size(5)
                .sortBy("dueDate")
                .sortDirection(SortDirection.DESC)
                .build();
    }

    private static ProjectMilestone milestone() {
        return ProjectMilestone.builder()
                .id(MILESTONE_ID)
                .projectId(PROJECT_ID)
                .title(TITLE)
                .dueDate(DUE_DATE)
                .status("OPEN")
                .build();
    }

    private static ProjectMilestoneResponseDto response() {
        return ProjectMilestoneResponseDto.builder()
                .id(MILESTONE_ID)
                .projectId(PROJECT_ID)
                .title(TITLE)
                .status("OPEN")
                .build();
    }
}
