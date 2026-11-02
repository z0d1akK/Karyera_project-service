package com.zodiakk.projectservice.security;

import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Security")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("rejects an anonymous request")
    void shouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects a request with a malformed user id")
    void shouldRejectMalformedUserId() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .header(USER_ID_HEADER, "not-a-uuid")
                        .header(USER_ROLE_HEADER, "ADMIN")
                        .header(USER_EMAIL_HEADER, "admin@company.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects a request with incomplete gateway headers")
    void shouldRejectIncompleteHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .header(USER_ID_HEADER, UUID.randomUUID().toString())
                        .header(USER_ROLE_HEADER, "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects an unauthenticated write")
    void shouldRejectAnonymousWrite() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("serves the health endpoint anonymously")
    void shouldServeHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("allows CORS preflight requests anonymously")
    void shouldAllowOptionsRequests() throws Exception {
        mockMvc.perform(options("/api/v1/projects")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("requires an internal key for the internal API")
    void shouldProtectInternalApi() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/internal/v1/projects/{projectId}/work-eligibility", projectId)
                        .param("employeeId", UUID.randomUUID().toString())
                        .param("workDate", "2026-01-01"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/internal/v1/projects/{projectId}/work-eligibility", projectId)
                        .header(INTERNAL_KEY_HEADER, "wrong-key")
                        .param("employeeId", UUID.randomUUID().toString())
                        .param("workDate", "2026-01-01"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(asAdmin(get("/internal/v1/projects/{projectId}/work-eligibility", projectId)
                        .param("employeeId", UUID.randomUUID().toString())
                        .param("workDate", "2026-01-01")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("does not accept the internal key on the public API")
    void shouldNotAuthenticatePublicApiWithInternalKey() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .header(INTERNAL_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("forbids a catalog write for a non-admin")
    void shouldForbidCatalogWriteForEmployee() throws Exception {
        mockMvc.perform(asEmployee(post("/api/v1/project-roles"), UUID.randomUUID())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"DEV","name":"Developer"}
                                """))
                .andExpect(status().isForbidden());
    }
}
