package com.zodiakk.projectservice.infrastructure;

import com.zodiakk.projectservice.common.AbstractIntegrationTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Test infrastructure")
class InfrastructureIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PostgreSQLContainer postgresContainer;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    @Qualifier("redisContainer")
    private GenericContainer<?> redisContainer;

    @Test
    @DisplayName("uses a PostgreSQL 17 container with the Liquibase schema")
    void shouldRunPostgres17() {
        assertThat(postgresContainer.isRunning()).isTrue();
        assertThat(postgresContainer.getDockerImageName()).startsWith("postgres:17");

        String version = jdbcTemplate.queryForObject("select version()", String.class);
        assertThat(version).contains("PostgreSQL 17");
        assertThat(jdbcTemplate.queryForObject("select current_database()", String.class))
                .isEqualTo("karyera_project_service_db");

        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class
        );

        assertThat(tables).contains(
                "projects",
                "project_roles",
                "project_assignments",
                "project_boards",
                "board_columns",
                "task_types",
                "project_tasks",
                "task_assignees",
                "task_relations",
                "task_comments",
                "task_activities",
                "task_skills",
                "project_links",
                "project_milestones",
                "outbox_events",
                "processed_events"
        );
    }

    @Test
    @DisplayName("runs an Apache Kafka container")
    void shouldRunKafka() throws Exception {
        assertThat(kafkaContainer.isRunning()).isTrue();
        assertThat(kafkaContainer.getDockerImageName()).startsWith("apache/kafka:4.3.1");

        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaContainer.getBootstrapServers()
        ))) {
            assertThat(adminClient.describeCluster().clusterId().get(15, TimeUnit.SECONDS)).isNotBlank();
        }
    }

    @Test
    @DisplayName("runs a Redis container")
    void shouldRunRedis() throws Exception {
        assertThat(redisContainer.isRunning()).isTrue();
        assertThat(redisContainer.getDockerImageName()).startsWith("redis:8.6.2");
        assertThat(pingRedis()).isEqualTo("+PONG");
    }

    private String pingRedis() throws Exception {
        try (Socket socket = new Socket(redisContainer.getHost(), redisContainer.getMappedPort(6379))) {
            socket.setSoTimeout(5_000);
            OutputStream output = socket.getOutputStream();
            output.write("PING\r\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                    .readLine();
        }
    }
}
