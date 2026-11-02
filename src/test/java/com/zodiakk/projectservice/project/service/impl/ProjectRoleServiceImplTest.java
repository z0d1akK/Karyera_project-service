package com.zodiakk.projectservice.project.service.impl;

import com.zodiakk.projectservice.common.dto.PageRequestDto;
import com.zodiakk.projectservice.common.dto.SortDirection;
import com.zodiakk.projectservice.project.dto.request.create.ProjectRoleCreateRequestDto;
import com.zodiakk.projectservice.project.dto.request.filter.ProjectRoleFilterDto;
import com.zodiakk.projectservice.project.dto.request.update.ProjectRoleUpdateRequestDto;
import com.zodiakk.projectservice.project.dto.response.ProjectRoleResponseDto;
import com.zodiakk.projectservice.project.entity.ProjectRole;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleCodeAlreadyExistsException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleInUseException;
import com.zodiakk.projectservice.project.exception.projectrole.ProjectRoleNotFoundException;
import com.zodiakk.projectservice.project.mapper.ProjectRoleMapper;
import com.zodiakk.projectservice.project.repository.ProjectAssignmentRepository;
import com.zodiakk.projectservice.project.repository.ProjectRoleRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectRoleServiceImplTest {

    private static final UUID ROLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String CODE = "DEV";

    private static final String NAME = "Developer";

    private static final String DESCRIPTION = "Builds the product";

    @Mock
    private ProjectRoleRepository projectRoleRepository;

    @Mock
    private ProjectAssignmentRepository projectAssignmentRepository;

    @Mock
    private ProjectRoleMapper projectRoleMapper;

    @InjectMocks
    private ProjectRoleServiceImpl projectRoleService;

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("maps a filtered page and preserves paging metadata")
        void shouldReturnMappedPage() {
            ProjectRoleFilterDto filter = ProjectRoleFilterDto.builder()
                    .code("de")
                    .name("dev")
                    .active(true)
                    .build();
            PageRequestDto pageRequest = PageRequestDto.builder()
                    .page(1)
                    .size(5)
                    .sortBy("name")
                    .sortDirection(SortDirection.DESC)
                    .build();
            ProjectRole entity = role(CODE, true);
            ProjectRoleResponseDto expected = response(CODE, true);
            Page<ProjectRole> page = new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 6);

            when(projectRoleRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            Page<ProjectRoleResponseDto> result = projectRoleService.findAll(filter, pageRequest);

            assertThat(result.getContent()).containsExactly(expected);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(5);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(projectRoleRepository).findAll(any(Specification.class), pageableCaptor.capture());

            Pageable pageable = pageableCaptor.getValue();
            assertThat(pageable.getPageNumber()).isEqualTo(1);
            assertThat(pageable.getPageSize()).isEqualTo(5);
            assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
            assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("returns an empty page without calling the mapper")
        void shouldReturnEmptyPage() {
            ProjectRoleFilterDto filter = ProjectRoleFilterDto.builder().name("missing").build();
            PageRequestDto pageRequest = PageRequestDto.builder().page(0).size(20).build();

            when(projectRoleRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<ProjectRoleResponseDto> result = projectRoleService.findAll(filter, pageRequest);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            verify(projectRoleMapper, never()).toResponseDto(any());
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("returns the mapped role when it exists")
        void shouldReturnRole() {
            ProjectRole entity = role(CODE, true);
            ProjectRoleResponseDto expected = response(CODE, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.getById(ROLE_ID)).isSameAs(expected);
        }

        @Test
        @DisplayName("throws when the role does not exist")
        void shouldThrowWhenMissing() {
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectRoleService.getById(ROLE_ID))
                    .isInstanceOf(ProjectRoleNotFoundException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verifyNoInteractions(projectRoleMapper);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves an active role when active is omitted")
        void shouldDefaultActiveToTrue() {
            ProjectRoleCreateRequestDto request = createRequest(null);
            ProjectRole entity = role(CODE, false);
            ProjectRoleResponseDto expected = response(CODE, true);

            when(projectRoleRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(projectRoleMapper.toEntity(request)).thenReturn(entity);
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.create(request)).isSameAs(expected);
            assertThat(entity.getActive()).isTrue();
        }

        @Test
        @DisplayName("keeps an explicitly active role active")
        void shouldKeepExplicitActiveFlag() {
            ProjectRoleCreateRequestDto request = createRequest(true);
            ProjectRole entity = role(CODE, false);
            ProjectRoleResponseDto expected = response(CODE, true);

            when(projectRoleRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(projectRoleMapper.toEntity(request)).thenReturn(entity);
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.create(request)).isSameAs(expected);
            assertThat(entity.getActive()).isTrue();
        }

        @Test
        @DisplayName("saves an inactive role when requested")
        void shouldCreateInactiveRole() {
            ProjectRoleCreateRequestDto request = createRequest(false);
            ProjectRole entity = role(CODE, true);
            ProjectRoleResponseDto expected = response(CODE, false);

            when(projectRoleRepository.existsByCodeIgnoreCase(CODE)).thenReturn(false);
            when(projectRoleMapper.toEntity(request)).thenReturn(entity);
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.create(request)).isSameAs(expected);
            assertThat(entity.getActive()).isFalse();
        }

        @Test
        @DisplayName("rejects a duplicate code")
        void shouldRejectDuplicateCode() {
            ProjectRoleCreateRequestDto request = createRequest(true);

            when(projectRoleRepository.existsByCodeIgnoreCase(CODE)).thenReturn(true);

            assertThatThrownBy(() -> projectRoleService.create(request))
                    .isInstanceOf(ProjectRoleCodeAlreadyExistsException.class)
                    .hasMessageContaining(CODE);

            verify(projectRoleRepository, never()).save(any());
            verifyNoInteractions(projectRoleMapper);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("updates a role when the code is unchanged")
        void shouldUpdateWhenCodeMatchesIgnoringCase() {
            ProjectRole entity = role(CODE, true);
            ProjectRoleUpdateRequestDto request = updateRequest("dev", "Engineer", false);
            ProjectRoleResponseDto expected = response("dev", false);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.update(ROLE_ID, request)).isSameAs(expected);

            verify(projectRoleRepository, never()).existsByCodeIgnoreCase(anyString());
            verify(projectRoleMapper).updateEntity(request, entity);
            verify(projectRoleRepository).save(entity);
        }

        @Test
        @DisplayName("skips the uniqueness check when the code is null")
        void shouldSkipUniquenessWhenCodeIsNull() {
            ProjectRole entity = role(CODE, true);
            ProjectRoleUpdateRequestDto request = updateRequest(null, "Engineer", null);
            ProjectRoleResponseDto expected = response(CODE, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.update(ROLE_ID, request)).isSameAs(expected);
            verify(projectRoleRepository, never()).existsByCodeIgnoreCase(anyString());
        }

        @Test
        @DisplayName("skips the uniqueness check when the code is blank")
        void shouldSkipUniquenessWhenCodeIsBlank() {
            ProjectRole entity = role(CODE, true);
            ProjectRoleUpdateRequestDto request = updateRequest("   ", NAME, true);
            ProjectRoleResponseDto expected = response(CODE, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.update(ROLE_ID, request)).isSameAs(expected);
            verify(projectRoleRepository, never()).existsByCodeIgnoreCase(anyString());
        }

        @Test
        @DisplayName("updates a role when the new code is unique")
        void shouldUpdateWhenNewCodeIsUnique() {
            ProjectRole entity = role(CODE, true);
            ProjectRoleUpdateRequestDto request = updateRequest("QA", "Quality", true);
            ProjectRoleResponseDto expected = response("QA", true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.existsByCodeIgnoreCase("QA")).thenReturn(false);
            when(projectRoleRepository.save(entity)).thenReturn(entity);
            when(projectRoleMapper.toResponseDto(entity)).thenReturn(expected);

            assertThat(projectRoleService.update(ROLE_ID, request)).isSameAs(expected);
            verify(projectRoleMapper).updateEntity(request, entity);
        }

        @Test
        @DisplayName("rejects a code that already belongs to another role")
        void shouldRejectDuplicateCode() {
            ProjectRole entity = role(CODE, true);
            ProjectRoleUpdateRequestDto request = updateRequest("QA", NAME, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectRoleRepository.existsByCodeIgnoreCase("QA")).thenReturn(true);

            assertThatThrownBy(() -> projectRoleService.update(ROLE_ID, request))
                    .isInstanceOf(ProjectRoleCodeAlreadyExistsException.class)
                    .hasMessageContaining("QA");

            verify(projectRoleMapper, never()).updateEntity(any(), any());
            verify(projectRoleRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the role does not exist")
        void shouldThrowWhenMissing() {
            ProjectRoleUpdateRequestDto request = updateRequest("QA", NAME, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectRoleService.update(ROLE_ID, request))
                    .isInstanceOf(ProjectRoleNotFoundException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verify(projectRoleRepository, never()).existsByCodeIgnoreCase(anyString());
            verifyNoInteractions(projectRoleMapper);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes a role that is not used by assignments")
        void shouldDeleteUnusedRole() {
            ProjectRole entity = role(CODE, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectAssignmentRepository.countByProjectRoleId(ROLE_ID)).thenReturn(0L);

            projectRoleService.delete(ROLE_ID);

            verify(projectRoleRepository).delete(entity);
        }

        @Test
        @DisplayName("rejects a role that is still assigned")
        void shouldRejectRoleInUse() {
            ProjectRole entity = role(CODE, true);

            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(entity));
            when(projectAssignmentRepository.countByProjectRoleId(ROLE_ID)).thenReturn(2L);

            assertThatThrownBy(() -> projectRoleService.delete(ROLE_ID))
                    .isInstanceOf(ProjectRoleInUseException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verify(projectRoleRepository, never()).delete(any(ProjectRole.class));
        }

        @Test
        @DisplayName("throws when the role does not exist")
        void shouldThrowWhenMissing() {
            when(projectRoleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectRoleService.delete(ROLE_ID))
                    .isInstanceOf(ProjectRoleNotFoundException.class)
                    .hasMessageContaining(ROLE_ID.toString());

            verify(projectAssignmentRepository, never()).countByProjectRoleId(any());
            verify(projectRoleRepository, never()).delete(any(ProjectRole.class));
        }
    }

    private static ProjectRoleCreateRequestDto createRequest(Boolean active) {
        return ProjectRoleCreateRequestDto.builder()
                .code(CODE)
                .name(NAME)
                .description(DESCRIPTION)
                .active(active)
                .build();
    }

    private static ProjectRoleUpdateRequestDto updateRequest(String code, String name, Boolean active) {
        return ProjectRoleUpdateRequestDto.builder()
                .code(code)
                .name(name)
                .description(DESCRIPTION)
                .active(active)
                .build();
    }

    private static ProjectRole role(String code, Boolean active) {
        return ProjectRole.builder()
                .id(ROLE_ID)
                .code(code)
                .name(NAME)
                .description(DESCRIPTION)
                .active(active)
                .build();
    }

    private static ProjectRoleResponseDto response(String code, Boolean active) {
        return ProjectRoleResponseDto.builder()
                .id(ROLE_ID)
                .code(code)
                .name(NAME)
                .description(DESCRIPTION)
                .active(active)
                .build();
    }
}
