package com.zodiakk.projectservice.integration.service;

import com.zodiakk.projectservice.common.cache.CacheNames;
import com.zodiakk.projectservice.common.dto.EmployeeReferenceDto;
import com.zodiakk.projectservice.integration.client.EmployeeServiceClient;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupItem;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupRequest;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupResponse;
import com.zodiakk.projectservice.integration.exception.EmployeeReferenceNotFoundException;
import com.zodiakk.projectservice.integration.exception.EmployeeServiceUnavailableException;
import com.zodiakk.projectservice.security.util.SecurityUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeDirectoryService {

    private static final int LOOKUP_BATCH_SIZE = 200;

    private final EmployeeServiceClient employeeServiceClient;

    @Cacheable(
            cacheNames = CacheNames.CURRENT_EMPLOYEE,
            key = "T(com.zodiakk.projectservice.security.util.SecurityUtils).getCurrentUserId()"
    )
    public EmployeeReferenceDto getCurrentEmployee() {
        return getByUserId(SecurityUtils.getCurrentUserId());
    }

    public EmployeeReferenceDto getByUserId(UUID userId) {
        try {
            return toReference(employeeServiceClient.getByUserId(userId));
        } catch (FeignException.NotFound ex) {
            throw new EmployeeReferenceNotFoundException(userId);
        } catch (FeignException ex) {
            log.warn("Employee service lookup by user id failed: userId={}", userId, ex);
            throw new EmployeeServiceUnavailableException(ex);
        }
    }

    public Optional<UUID> findCurrentEmployeeId() {
        try {
            return Optional.of(getCurrentEmployee().getEmployeeId());
        } catch (EmployeeReferenceNotFoundException | EmployeeServiceUnavailableException ex) {
            return Optional.empty();
        }
    }

    public Map<UUID, EmployeeReferenceDto> resolveAll(Collection<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }

        try {
            return index(lookupStrict(employeeIds));
        } catch (RuntimeException ex) {
            log.warn("Employee service batch lookup failed, continuing without enrichment", ex);
            return Map.of();
        }
    }

    public Map<UUID, EmployeeReferenceDto> requireAll(Collection<UUID> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, EmployeeReferenceDto> resolved = index(lookupStrict(employeeIds));

        for (UUID employeeId : new LinkedHashSet<>(employeeIds)) {
            if (!resolved.containsKey(employeeId)) {
                throw new EmployeeReferenceNotFoundException(employeeId);
            }
        }

        return resolved;
    }

    public EmployeeReferenceDto requireById(UUID employeeId) {
        return requireAll(List.of(employeeId)).get(employeeId);
    }

    private Map<UUID, EmployeeReferenceDto> index(List<EmployeeReferenceDto> references) {
        return references.stream()
                .collect(Collectors.toMap(
                        EmployeeReferenceDto::getEmployeeId,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    private List<EmployeeReferenceDto> lookupStrict(Collection<UUID> employeeIds) {
        LinkedHashSet<UUID> uniqueIds = new LinkedHashSet<>(employeeIds);

        if (uniqueIds.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = new ArrayList<>(uniqueIds);

        if (ids.size() > LOOKUP_BATCH_SIZE) {
            List<EmployeeReferenceDto> result = new ArrayList<>();

            for (int from = 0; from < ids.size(); from += LOOKUP_BATCH_SIZE) {
                int to = Math.min(from + LOOKUP_BATCH_SIZE, ids.size());
                result.addAll(lookupStrict(ids.subList(from, to)));
            }

            return result;
        }

        try {
            EmployeeLookupResponse response = employeeServiceClient.lookup(
                    new EmployeeLookupRequest(ids)
            );

            if (response == null || response.employees() == null) {
                return List.of();
            }

            return response.employees().stream()
                    .map(this::toReference)
                    .toList();

        } catch (FeignException ex) {
            log.warn("Employee service batch lookup failed", ex);
            throw new EmployeeServiceUnavailableException(ex);
        }
    }

    private EmployeeReferenceDto toReference(EmployeeLookupItem item) {
        return EmployeeReferenceDto.builder()
                .employeeId(item.id())
                .fullName(item.fullName())
                .active(item.active())
                .build();
    }
}
