package com.zodiakk.projectservice.common;

import com.zodiakk.projectservice.TestcontainersConfiguration;
import com.zodiakk.projectservice.common.cache.CacheNames;
import com.zodiakk.projectservice.common.constants.Headers;
import com.zodiakk.projectservice.integration.client.EmployeeServiceClient;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupItem;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupRequest;
import com.zodiakk.projectservice.integration.dto.EmployeeLookupResponse;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String USER_ID_HEADER = Headers.USER_ID;

    protected static final String USER_ROLE_HEADER = Headers.USER_ROLE;

    protected static final String USER_EMAIL_HEADER = Headers.USER_EMAIL;

    protected static final String INTERNAL_KEY_HEADER = Headers.INTERNAL_KEY;

    protected static final String INTERNAL_API_KEY = "test-internal-api-key";

    private final Map<UUID, EmployeeLookupItem> employeesById = new HashMap<>();

    private final Map<UUID, EmployeeLookupItem> employeesByUserId = new HashMap<>();

    @MockitoBean
    protected EmployeeServiceClient employeeServiceClient;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected CacheManager cacheManager;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                () -> TestcontainersConfiguration.kafka().getBootstrapServers()
        );
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE task_skills, task_relations, task_comments, task_activities, task_assignees,
                    project_tasks, board_columns, project_boards, project_assignments, project_milestones,
                    project_links, projects, project_roles, task_types, processed_events, outbox_events
                    CASCADE
                """);

        List.of(
                CacheNames.PROJECTS,
                CacheNames.PROJECT_ROLES,
                CacheNames.TASK_TYPES,
                CacheNames.BOARDS,
                CacheNames.TASKS,
                CacheNames.EMPLOYEE_DIRECTORY,
                CacheNames.CURRENT_EMPLOYEE
        ).forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

        employeesById.clear();
        employeesByUserId.clear();
        stubEmployeeClient();
    }

    protected String toJson(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    protected EmployeeLookupItem registerEmployee(UUID userId) {
        EmployeeLookupItem item = new EmployeeLookupItem(
                UUID.randomUUID(),
                "Ivan Petrov",
                true,
                null,
                null
        );
        employeesById.put(item.id(), item);
        if (userId != null) {
            employeesByUserId.put(userId, item);
        }
        return item;
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asUser(
            B builder,
            UUID userId,
            String role
    ) {
        return builder
                .header(USER_ID_HEADER, userId.toString())
                .header(USER_ROLE_HEADER, role)
                .header(USER_EMAIL_HEADER, "user-" + userId + "@company.com");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asHr(B builder) {
        return asUser(builder, UUID.randomUUID(), "HR");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asAdmin(B builder) {
        return asUser(builder, UUID.randomUUID(), "ADMIN");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asEmployee(B builder, UUID userId) {
        return asUser(builder, userId, "EMPLOYEE");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asManager(B builder) {
        return asUser(builder, UUID.randomUUID(), "MANAGER");
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B asInternal(B builder) {
        return builder.header(INTERNAL_KEY_HEADER, INTERNAL_API_KEY);
    }

    private void stubEmployeeClient() {
        lenient().when(employeeServiceClient.lookup(any(EmployeeLookupRequest.class)))
                .thenAnswer(invocation -> {
                    EmployeeLookupRequest request = invocation.getArgument(0);
                    List<EmployeeLookupItem> found = request.employeeIds() == null
                            ? List.of()
                            : request.employeeIds().stream()
                            .filter(employeesById::containsKey)
                            .map(employeesById::get)
                            .toList();
                    return new EmployeeLookupResponse(found);
                });

        lenient().when(employeeServiceClient.getByUserId(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID userId = invocation.getArgument(0);
                    EmployeeLookupItem item = employeesByUserId.get(userId);
                    if (item == null) {
                        throw new FeignException.NotFound(
                                "missing",
                                Request.create(
                                        Request.HttpMethod.GET,
                                        "/internal/v1/employees/by-user/" + userId,
                                        Map.of(),
                                        null,
                                        StandardCharsets.UTF_8,
                                        null
                                ),
                                null,
                                Map.of()
                        );
                    }
                    return item;
                });
    }
}
