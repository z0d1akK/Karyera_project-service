package com.zodiakk.projectservice.common.cache;

import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Redis caching")
class CacheIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("caches a project role after it is read")
    void shouldCacheProjectRole() throws Exception {
        String body = mockMvc.perform(asAdmin(post("/api/v1/project-roles"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEV","name":"Developer","active":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(body).get("id").asString();

        mockMvc.perform(asAdmin(get("/api/v1/project-roles/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DEV"));

        Cache cache = cacheManager.getCache(CacheNames.PROJECT_ROLES);
        assertThat(cache).isNotNull();
        assertThat(cache.get(java.util.UUID.fromString(id))).isNotNull();
    }
}
