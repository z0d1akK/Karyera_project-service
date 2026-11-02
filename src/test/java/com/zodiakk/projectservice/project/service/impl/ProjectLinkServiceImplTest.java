package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.project.dto.request.create.ProjectLinkCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectLinkUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectLinkResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectLink;
import com.zodiakk.projectservice.project.exception.link.ProjectLinkNotFoundException;
import com.zodiakk.projectservice.project.exception.project.ProjectNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectLinkMapper;
import com.zodiakk.projectservice.project.repository.ProjectLinkRepository;
import com.zodiakk.projectservice.project.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ProjectLinkServiceImplTest {

    private static final UUID PROJECT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID OTHER_PROJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID LINK_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final UUID SECOND_LINK_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final String LABEL = "Wiki";

    private static final String URL = "https://wiki.example/apollo";

    @Mock
    private ProjectLinkRepository projectLinkRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectLinkMapper projectLinkMapper;

    @InjectMocks
    private ProjectLinkServiceImpl projectLinkService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("maps links in repository order")
        void shouldReturnMappedLinks() {
            ProjectLink first = link(LINK_ID, LABEL);
            ProjectLink second = link(SECOND_LINK_ID, "Board");
            ProjectLinkResponseDto firstResponse = response(LINK_ID, LABEL);
            ProjectLinkResponseDto secondResponse = response(SECOND_LINK_ID, "Board");

            when(projectLinkRepository.findAllByProjectIdOrderByCreatedAtAsc(PROJECT_ID))
                    .thenReturn(List.of(first, second));
            when(projectLinkMapper.toResponseDto(first)).thenReturn(firstResponse);
            when(projectLinkMapper.toResponseDto(second)).thenReturn(secondResponse);

            assertThat(projectLinkService.findAll(PROJECT_ID)).containsExactly(firstResponse, secondResponse);
        }

        @Test
        @DisplayName("returns an empty list without calling the mapper")
        void shouldReturnEmptyList() {
            when(projectLinkRepository.findAllByProjectIdOrderByCreatedAtAsc(PROJECT_ID)).thenReturn(List.of());

            assertThat(projectLinkService.findAll(PROJECT_ID)).isEmpty();

            verify(projectLinkMapper, never()).toResponseDto(any());
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves a link for an existing project and stamps the path project id")
        void shouldCreateLink() {
            ProjectLinkCreateRequestDto request = ProjectLinkCreateRequestDto.builder()
                    .projectId(OTHER_PROJECT_ID)
                    .label(LABEL)
                    .url(URL)
                    .build();
            ProjectLink entity = link(LINK_ID, LABEL);
            ProjectLinkResponseDto expected = response(LINK_ID, LABEL);

            when(projectRepository.existsById(PROJECT_ID)).thenReturn(true);
            when(projectLinkMapper.toEntity(request)).thenReturn(entity);
            when(projectLinkRepository.save(entity)).thenReturn(entity);
            when(projectLinkMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectLinkResponseDto result = projectLinkService.create(PROJECT_ID, request);

            assertThat(result).isSameAs(expected);
            assertThat(request.getProjectId()).isEqualTo(PROJECT_ID);
            verify(projectLinkMapper).toEntity(request);
            verify(projectLinkRepository).save(entity);
        }

        @Test
        @DisplayName("throws when the project does not exist")
        void shouldThrowWhenProjectIsMissing() {
            ProjectLinkCreateRequestDto request = ProjectLinkCreateRequestDto.builder()
                    .projectId(OTHER_PROJECT_ID)
                    .label(LABEL)
                    .url(URL)
                    .build();

            when(projectRepository.existsById(PROJECT_ID)).thenReturn(false);

            assertThatThrownBy(() -> projectLinkService.create(PROJECT_ID, request))
                    .isInstanceOf(ProjectNotFoundException.class)
                    .hasMessageContaining(PROJECT_ID.toString());

            assertThat(request.getProjectId()).isEqualTo(PROJECT_ID);
            verifyNoInteractions(projectLinkMapper);
            verify(projectLinkRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates an existing link")
        void shouldUpdateLink() {
            ProjectLink entity = link(LINK_ID, LABEL);
            ProjectLinkUpdateRequestDto request = ProjectLinkUpdateRequestDto.builder()
                    .label("Docs")
                    .url("https://docs.example/apollo")
                    .build();
            ProjectLinkResponseDto expected = response(LINK_ID, "Docs");

            when(projectLinkRepository.findById(LINK_ID)).thenReturn(Optional.of(entity));
            when(projectLinkRepository.save(entity)).thenReturn(entity);
            when(projectLinkMapper.toResponseDto(entity)).thenReturn(expected);

            ProjectLinkResponseDto result = projectLinkService.update(LINK_ID, request);

            assertThat(result).isSameAs(expected);
            verify(projectLinkMapper).updateEntity(request, entity);
            verify(projectLinkRepository).save(entity);
        }

        @Test
        @DisplayName("throws when the link does not exist")
        void shouldThrowWhenMissing() {
            ProjectLinkUpdateRequestDto request = ProjectLinkUpdateRequestDto.builder()
                    .label("Docs")
                    .build();

            when(projectLinkRepository.findById(LINK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectLinkService.update(LINK_ID, request))
                    .isInstanceOf(ProjectLinkNotFoundException.class)
                    .hasMessageContaining(LINK_ID.toString());

            verify(projectLinkMapper, never()).updateEntity(any(), any());
            verify(projectLinkRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes an existing link")
        void shouldDeleteLink() {
            ProjectLink entity = link(LINK_ID, LABEL);

            when(projectLinkRepository.findById(LINK_ID)).thenReturn(Optional.of(entity));

            projectLinkService.delete(LINK_ID);

            verify(projectLinkRepository).delete(entity);
        }

        @Test
        @DisplayName("throws when the link does not exist")
        void shouldThrowWhenMissing() {
            when(projectLinkRepository.findById(LINK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectLinkService.delete(LINK_ID))
                    .isInstanceOf(ProjectLinkNotFoundException.class)
                    .hasMessageContaining(LINK_ID.toString());

            verify(projectLinkRepository, never()).delete(any(ProjectLink.class));
        }
    }

    private static ProjectLink link(UUID id, String label) {
        return ProjectLink.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .label(label)
                .url(URL)
                .build();
    }

    private static ProjectLinkResponseDto response(UUID id, String label) {
        return ProjectLinkResponseDto.builder()
                .id(id)
                .projectId(PROJECT_ID)
                .label(label)
                .url(URL)
                .build();
    }
}
