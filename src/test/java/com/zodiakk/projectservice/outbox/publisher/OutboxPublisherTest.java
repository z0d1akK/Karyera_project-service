package com.zodiakk.projectservice.outbox.publisher;

import com.zodiakk.projectservice.outbox.config.OutboxProperties;
import com.zodiakk.projectservice.outbox.entity.OutboxEvent;
import com.zodiakk.projectservice.outbox.entity.OutboxEventStatus;
import com.zodiakk.projectservice.outbox.service.OutboxClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final String TOPIC = "project-events";

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID AGGREGATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final String PAYLOAD = "{\"taskId\":\"cccccccc-cccc-cccc-cccc-cccccccccccc\"}";

    private static final long DEFAULT_SEND_TIMEOUT_SECONDS = 15L;

    @Mock
    private OutboxClaimService claimService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = publisherWithSendTimeout(DEFAULT_SEND_TIMEOUT_SECONDS);
    }

    @Nested
    @DisplayName("publishPendingEvents")
    class PublishPendingEvents {

        @Test
        @DisplayName("sends each claimed event and marks it published when Kafka acknowledges it")
        void shouldPublishClaimedEvent() {
            OutboxEvent event = event(2);
            CompletableFuture<SendResult<String, String>> acknowledged =
                    CompletableFuture.completedFuture(null);

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(acknowledged);
            when(claimService.markPublished(EVENT_ID, 2)).thenReturn(true);

            publisher.publishPendingEvents();

            verify(kafkaTemplate).send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD);
            verify(claimService).markPublished(EVENT_ID, 2);
            verify(claimService, never()).markFailed(any(), anyInt());
        }

        @Test
        @DisplayName("does nothing when the claim service returns an empty batch")
        void shouldDoNothingForEmptyBatch() {
            when(claimService.claimBatch()).thenReturn(List.of());

            publisher.publishPendingEvents();

            verify(claimService).claimBatch();
            verifyNoInteractions(kafkaTemplate);
            verify(claimService, never()).markPublished(any(), anyInt());
            verify(claimService, never()).markFailed(any(), anyInt());
        }

        @Test
        @DisplayName("publishes every event in the claimed batch")
        void shouldPublishEveryClaimedEvent() {
            OutboxEvent first = event(1);
            OutboxEvent second = event(2);
            CompletableFuture<SendResult<String, String>> acknowledged =
                    CompletableFuture.completedFuture(null);

            when(claimService.claimBatch()).thenReturn(List.of(first, second));
            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(acknowledged);
            when(claimService.markPublished(EVENT_ID, 1)).thenReturn(true);
            when(claimService.markPublished(EVENT_ID, 2)).thenReturn(true);

            publisher.publishPendingEvents();

            verify(kafkaTemplate, times(2)).send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD);
            verify(claimService).markPublished(EVENT_ID, 1);
            verify(claimService).markPublished(EVENT_ID, 2);
        }
    }

    @Nested
    @DisplayName("completion handling")
    class CompletionHandling {

        @Test
        @DisplayName("logs a warning but does not fail when the claim expired before markPublished")
        void shouldTolerateExpiredClaim() {
            OutboxEvent event = event(1);
            CompletableFuture<SendResult<String, String>> acknowledged =
                    CompletableFuture.completedFuture(null);

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(acknowledged);
            when(claimService.markPublished(EVENT_ID, 1)).thenReturn(false);

            assertThatCode(() -> publisher.publishPendingEvents()).doesNotThrowAnyException();

            verify(claimService).markPublished(EVENT_ID, 1);
            verify(claimService, never()).markFailed(any(), anyInt());
        }

        @Test
        @DisplayName("swallows failures raised while marking an event as published")
        void shouldSwallowMarkPublishedFailures() {
            OutboxEvent event = event(1);
            CompletableFuture<SendResult<String, String>> acknowledged =
                    CompletableFuture.completedFuture(null);

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(acknowledged);
            when(claimService.markPublished(EVENT_ID, 1))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThatCode(() -> publisher.publishPendingEvents()).doesNotThrowAnyException();

            verify(claimService).markPublished(EVENT_ID, 1);
        }
    }

    @Nested
    @DisplayName("failure handling")
    class FailureHandling {

        @Test
        @DisplayName("marks the event failed when the send future completes exceptionally")
        void shouldMarkFailedWhenSendCompletesExceptionally() {
            OutboxEvent event = event(2);
            CompletableFuture<SendResult<String, String>> failed =
                    CompletableFuture.failedFuture(new RuntimeException("broker rejected the record"));

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(failed);
            when(claimService.markFailed(EVENT_ID, 2)).thenReturn(true);

            publisher.publishPendingEvents();

            verify(claimService).markFailed(EVENT_ID, 2);
            verify(claimService, never()).markPublished(any(), anyInt());
        }

        @Test
        @DisplayName("marks the event failed when kafkaTemplate.send throws synchronously")
        void shouldMarkFailedWhenSendThrowsSynchronously() {
            OutboxEvent event = event(3);

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD))
                    .thenThrow(new KafkaException("producer is closed"));
            when(claimService.markFailed(EVENT_ID, 3)).thenReturn(true);

            assertThatCode(() -> publisher.publishPendingEvents()).doesNotThrowAnyException();

            verify(claimService).markFailed(EVENT_ID, 3);
            verify(claimService, never()).markPublished(any(), anyInt());
        }

        @Test
        @DisplayName("marks the event failed when the send times out")
        void shouldMarkFailedWhenSendTimesOut() {
            OutboxPublisher shortTimeoutPublisher = publisherWithSendTimeout(0L);
            OutboxEvent event = event(1);
            CompletableFuture<SendResult<String, String>> neverCompletes = new CompletableFuture<>();

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD))
                    .thenReturn(neverCompletes);

            shortTimeoutPublisher.publishPendingEvents();

            verify(claimService, timeout(5_000)).markFailed(EVENT_ID, 1);
            verify(claimService, never()).markPublished(any(), anyInt());
        }

        @Test
        @DisplayName("swallows failures raised while marking an event as failed")
        void shouldSwallowMarkFailedFailures() {
            OutboxEvent event = event(1);
            CompletableFuture<SendResult<String, String>> failed =
                    CompletableFuture.failedFuture(new RuntimeException("send failed"));

            when(claimService.claimBatch()).thenReturn(List.of(event));
            when(kafkaTemplate.send(TOPIC, AGGREGATE_ID.toString(), PAYLOAD)).thenReturn(failed);
            when(claimService.markFailed(EVENT_ID, 1))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThatCode(() -> publisher.publishPendingEvents()).doesNotThrowAnyException();

            verify(claimService).markFailed(EVENT_ID, 1);
        }
    }

    private OutboxPublisher publisherWithSendTimeout(long sendTimeoutSeconds) {
        OutboxProperties properties = new OutboxProperties(
                10,
                2_000L,
                180L,
                sendTimeoutSeconds
        );
        OutboxPublisher result = new OutboxPublisher(claimService, kafkaTemplate, properties);
        ReflectionTestUtils.setField(result, "projectEventsTopic", TOPIC);
        return result;
    }

    private OutboxEvent event(int attempts) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return OutboxEvent.builder()
                .id(EVENT_ID)
                .aggregateId(AGGREGATE_ID)
                .aggregateType("ProjectTask")
                .eventType("ProjectTaskCompleted")
                .eventVersion(1)
                .payload(PAYLOAD)
                .status(OutboxEventStatus.PROCESSING)
                .attempts(attempts)
                .createdAt(now)
                .nextAttemptAt(now)
                .build();
    }
}
