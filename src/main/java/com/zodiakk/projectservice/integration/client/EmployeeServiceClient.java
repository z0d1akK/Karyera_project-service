package com.zodiakk.projectservice.integration.client;

import com.zodiakk.projectservice.integration.dto.EmployeeLookupItem;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupRequest;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "employee-service",
        url = "${services.employee.url}",
        configuration = EmployeeClientConfig.class
)
public interface EmployeeServiceClient {

    @PostMapping("/internal/v1/employees/lookup")
    EmployeeLookupResponse lookup(@RequestBody EmployeeLookupRequest request);

    @GetMapping("/internal/v1/employees/by-user/{userId}")
    EmployeeLookupItem getByUserId(@PathVariable("userId") UUID userId);
}
