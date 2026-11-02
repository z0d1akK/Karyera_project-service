package com.zodiakk.projectservice.integration.service;

import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.integration.client.EmployeeServiceClient;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupItem;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupRequest;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupResponse;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceNotFoundException;
import com.zodiakk.projectservice.integration.exception.EmployeeServiceUnavailableException;
import com.zodiakk.projectservice.security.principal.CurrentUser;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDirectoryServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID EMPLOYEE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID OTHER_EMPLOYEE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private EmployeeServiceClient employeeServiceClient;

    @InjectMocks
    private EmployeeDirectoryService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getByUserId")
    class GetByUserId {

        @Test
        @DisplayName("maps a directory item to an employee reference")
        void shouldMapEmployeeReference() {
            when(employeeServiceClient.getByUserId(USER_ID)).thenReturn(item(EMPLOYEE_ID, "Ada Lovelace", false));

            EmployeeReferenceDto reference = service.getByUserId(USER_ID);

            assertThat(reference.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(reference.getFullName()).isEqualTo("Ada Lovelace");
            assertThat(reference.getActive()).isFalse();
        }

        @Test
        @DisplayName("translates a 404 from the employee service into a missing reference")
        void shouldTranslateNotFound() {
            when(employeeServiceClient.getByUserId(USER_ID)).thenThrow(mock(FeignException.NotFound.class));

            assertThatThrownBy(() -> service.getByUserId(USER_ID))
                    .isInstanceOf(EmployeeReferenceNotFoundException.class);
        }

        @Test
        @DisplayName("translates other Feign failures into an unavailable employee service")
        void shouldTranslateOtherFeignErrors() {
            FeignException failure = mock(FeignException.InternalServerError.class);
            when(employeeServiceClient.getByUserId(USER_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getByUserId(USER_ID))
                    .isInstanceOf(EmployeeServiceUnavailableException.class)
                    .hasCause(failure);
        }
    }

    @Nested
    @DisplayName("getCurrentEmployee")
    class GetCurrentEmployee {

        @Test
        @DisplayName("resolves the authenticated user through the employee service")
        void shouldResolveAuthenticatedUser() {
            authenticate();
            when(employeeServiceClient.getByUserId(USER_ID)).thenReturn(item(EMPLOYEE_ID, "Ada Lovelace", true));

            EmployeeReferenceDto reference = service.getCurrentEmployee();

            assertThat(reference.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(reference.getFullName()).isEqualTo("Ada Lovelace");
            verify(employeeServiceClient).getByUserId(USER_ID);
        }

        @Test
        @DisplayName("fails when the security context has no current user")
        void shouldFailWhenUnauthenticated() {
            assertThatThrownBy(() -> service.getCurrentEmployee())
                    .isInstanceOf(IllegalStateException.class);

            verifyNoInteractions(employeeServiceClient);
        }
    }

    @Nested
    @DisplayName("findCurrentEmployeeId")
    class FindCurrentEmployeeId {

        @Test
        @DisplayName("returns the current employee id when the directory lookup succeeds")
        void shouldReturnCurrentEmployeeId() {
            authenticate();
            when(employeeServiceClient.getByUserId(USER_ID)).thenReturn(item(EMPLOYEE_ID, "Ada Lovelace", true));

            assertThat(service.findCurrentEmployeeId()).contains(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("returns empty when the current user has no employee reference")
        void shouldReturnEmptyWhenReferenceIsMissing() {
            authenticate();
            when(employeeServiceClient.getByUserId(USER_ID)).thenThrow(mock(FeignException.NotFound.class));

            assertThat(service.findCurrentEmployeeId()).isEmpty();
        }

        @Test
        @DisplayName("returns empty when the employee service is unavailable")
        void shouldReturnEmptyWhenServiceIsUnavailable() {
            authenticate();
            when(employeeServiceClient.getByUserId(USER_ID))
                    .thenThrow(mock(FeignException.ServiceUnavailable.class));

            assertThat(service.findCurrentEmployeeId()).isEmpty();
        }

        @Test
        @DisplayName("propagates a missing security context")
        void shouldPropagateMissingSecurityContext() {
            assertThatThrownBy(() -> service.findCurrentEmployeeId())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("resolveAll")
    class ResolveAll {

        @Test
        @DisplayName("returns an empty map for a null or empty id collection")
        void shouldReturnEmptyForMissingIds() {
            assertThat(service.resolveAll(null)).isEmpty();
            assertThat(service.resolveAll(List.of())).isEmpty();

            verifyNoInteractions(employeeServiceClient);
        }

        @Test
        @DisplayName("indexes the employees returned by a strict lookup")
        void shouldIndexResolvedEmployees() {
            when(employeeServiceClient.lookup(any())).thenReturn(new EmployeeLookupResponse(List.of(
                    item(EMPLOYEE_ID, "First", true),
                    item(EMPLOYEE_ID, "Second", false),
                    item(OTHER_EMPLOYEE_ID, "Grace Hopper", true)
            )));

            Map<UUID, EmployeeReferenceDto> resolved = service.resolveAll(List.of(EMPLOYEE_ID, OTHER_EMPLOYEE_ID));

            assertThat(resolved).containsOnlyKeys(EMPLOYEE_ID, OTHER_EMPLOYEE_ID);
            assertThat(resolved.get(EMPLOYEE_ID).getFullName()).isEqualTo("First");
            assertThat(resolved.get(OTHER_EMPLOYEE_ID).getActive()).isTrue();
        }

        @Test
        @DisplayName("returns an empty map when the batch lookup fails")
        void shouldSwallowLookupFailures() {
            when(employeeServiceClient.lookup(any())).thenThrow(mock(FeignException.class));

            assertThat(service.resolveAll(List.of(EMPLOYEE_ID))).isEmpty();
        }

        @Test
        @DisplayName("returns an empty map when the lookup response has no employees")
        void shouldTreatMissingResponseAsEmpty() {
            when(employeeServiceClient.lookup(any())).thenReturn(null);

            assertThat(service.resolveAll(List.of(EMPLOYEE_ID))).isEmpty();

            when(employeeServiceClient.lookup(any())).thenReturn(new EmployeeLookupResponse(null));

            assertThat(service.resolveAll(List.of(EMPLOYEE_ID))).isEmpty();
        }
    }

    @Nested
    @DisplayName("requireAll")
    class RequireAll {

        @Test
        @DisplayName("returns an empty map for a null or empty id collection")
        void shouldReturnEmptyForMissingIds() {
            assertThat(service.requireAll(null)).isEmpty();
            assertThat(service.requireAll(List.of())).isEmpty();

            verifyNoInteractions(employeeServiceClient);
        }

        @Test
        @DisplayName("returns every requested employee")
        void shouldRequireEveryEmployee() {
            when(employeeServiceClient.lookup(any())).thenReturn(new EmployeeLookupResponse(List.of(
                    item(EMPLOYEE_ID, "Ada Lovelace", true)
            )));

            Map<UUID, EmployeeReferenceDto> resolved = service.requireAll(List.of(EMPLOYEE_ID, EMPLOYEE_ID));

            assertThat(resolved).containsOnlyKeys(EMPLOYEE_ID);
            verify(employeeServiceClient).lookup(any());
        }

        @Test
        @DisplayName("fails when any requested employee is absent from the lookup")
        void shouldFailWhenEmployeeIsMissing() {
            when(employeeServiceClient.lookup(any())).thenReturn(new EmployeeLookupResponse(List.of(
                    item(EMPLOYEE_ID, "Ada Lovelace", true)
            )));

            assertThatThrownBy(() -> service.requireAll(List.of(EMPLOYEE_ID, OTHER_EMPLOYEE_ID)))
                    .isInstanceOf(EmployeeReferenceNotFoundException.class);
        }

        @Test
        @DisplayName("propagates Feign failures as an unavailable employee service")
        void shouldPropagateFeignFailures() {
            FeignException failure = mock(FeignException.BadGateway.class);
            when(employeeServiceClient.lookup(any())).thenThrow(failure);

            assertThatThrownBy(() -> service.requireAll(List.of(EMPLOYEE_ID)))
                    .isInstanceOf(EmployeeServiceUnavailableException.class)
                    .hasCause(failure);
        }

        @Test
        @DisplayName("propagates unexpected runtime failures")
        void shouldPropagateUnexpectedFailures() {
            when(employeeServiceClient.lookup(any())).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> service.requireAll(List.of(EMPLOYEE_ID)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom");
        }

        @Test
        @DisplayName("looks up ids in chunks of 200")
        void shouldChunkLargeLookups() {
            List<UUID> ids = new ArrayList<>();
            for (int index = 0; index < 201; index++) {
                ids.add(new UUID(0L, index + 1L));
            }

            when(employeeServiceClient.lookup(any())).thenAnswer(invocation -> {
                EmployeeLookupRequest request = invocation.getArgument(0);
                List<EmployeeLookupItem> employees = request.employeeIds().stream()
                        .map(id -> item(id, "Employee " + id, true))
                        .toList();
                return new EmployeeLookupResponse(employees);
            });

            Map<UUID, EmployeeReferenceDto> resolved = service.requireAll(ids);

            assertThat(resolved).hasSize(201);

            ArgumentCaptor<EmployeeLookupRequest> captor = ArgumentCaptor.forClass(EmployeeLookupRequest.class);
            verify(employeeServiceClient, times(2)).lookup(captor.capture());
            assertThat(captor.getAllValues().get(0).employeeIds()).hasSize(200);
            assertThat(captor.getAllValues().get(1).employeeIds()).hasSize(1);
            verify(employeeServiceClient, never()).getByUserId(any());
        }
    }

    @Nested
    @DisplayName("requireById")
    class RequireById {

        @Test
        @DisplayName("returns the single required employee")
        void shouldRequireEmployeeById() {
            when(employeeServiceClient.lookup(any())).thenReturn(new EmployeeLookupResponse(List.of(
                    item(EMPLOYEE_ID, "Ada Lovelace", true)
            )));

            EmployeeReferenceDto reference = service.requireById(EMPLOYEE_ID);

            assertThat(reference.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
            assertThat(reference.getFullName()).isEqualTo("Ada Lovelace");
        }

        @Test
        @DisplayName("fails when the employee is not returned")
        void shouldFailWhenEmployeeIsMissing() {
            when(employeeServiceClient.lookup(any())).thenReturn(new EmployeeLookupResponse(List.of()));

            assertThatThrownBy(() -> service.requireById(EMPLOYEE_ID))
                    .isInstanceOf(EmployeeReferenceNotFoundException.class);
        }
    }

    private void authenticate() {
        CurrentUser currentUser = CurrentUser.builder()
                .userId(USER_ID)
                .role("USER")
                .email("ada@example.com")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null)
        );
    }

    private EmployeeLookupItem item(UUID id, String fullName, boolean active) {
        return new EmployeeLookupItem(id, fullName, active, null, null);
    }
}
