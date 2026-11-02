package com.zodiakk.projectservice;

import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import com.zodiakk.projectservice.common.cache.CacheNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Project service application context")
class ApplicationContextIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("loads the application context with infrastructure beans")
    void shouldLoadContext() {
        assertThat(applicationContext).isNotNull();
        assertThat(kafkaTemplate).isNotNull();
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCache(CacheNames.PROJECT_ROLES)).isNotNull();
        assertThat(cacheManager.getCache(CacheNames.TASK_TYPES)).isNotNull();
    }
}
