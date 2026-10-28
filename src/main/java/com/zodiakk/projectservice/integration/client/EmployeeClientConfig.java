package com.zodiakk.projectservice.integration.client;

import com.zodiakk.projectservice.common.constants.Headers;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class EmployeeClientConfig {

    @Bean
    public RequestInterceptor internalApiKeyInterceptor(
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return template -> template.header(Headers.INTERNAL_KEY, internalApiKey);
    }
}
