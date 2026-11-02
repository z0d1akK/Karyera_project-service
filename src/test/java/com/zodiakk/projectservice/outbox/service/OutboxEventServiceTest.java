package com.zodiakk.projectservice.outbox.service;

import com.zodiakk.projectservice.common.exception.ErrorMessages;
import com.zodiakk.projectservice.outbox.entity.OutboxEvent;
import com.zodiakk.projectservice.outbox.entity.OutboxEventStatus;
import com.zodiakk.projectservice.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    private static final UUID AGGREGATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final String AGGREGATE_TYPE = "ProjectTask";

    private static final String EVENT_TYPE = "ProjectTaskCompleted";

    private static final int EVENT_VERSION = 1;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutboxEventService service;

    @Nested
    @DisplayName("saveEvent")
    class SaveEvent {

        @Test
        @DisplayName("serializes the envelope and persists a pending outbox event")
        void shouldPersistPendingEvent() {
            Map<String, String> payload = Map.of("taskId", AGGREGATE_ID.toString());

            service.saveEvent(AGGREGATE_ID, AGGREGATE_TYPE, EVENT_TYPE, EVENT_VERSION, payload);

            ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxEventRepository).save(captor.capture());

            OutboxEvent saved = captor.getValue();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(saved.getAggregateType()).isEqualTo(AGGREGATE_TYPE);
            assertThat(saved.getEventType()).isEqualTo(EVENT_TYPE);
            assertThat(saved.getEventVersion()).isEqualTo(EVENT_VERSION);
            assertThat(saved.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat(saved.getAttempts()).isZero();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getNextAttemptAt()).isEqualTo(saved.getCreatedAt());
            assertThat(saved.getPublishedAt()).isNull();
            assertThat(saved.getLockedUntil()).isNull();

            JsonNode envelope = objectMapper.readTree(saved.getPayload());

            assertThat(envelope.get("eventId").asString()).isEqualTo(saved.getId().toString());
            assertThat(envelope.get("aggregateId").asString()).isEqualTo(AGGREGATE_ID.toString());
            assertThat(envelope.get("aggregateType").asString()).isEqualTo(AGGREGATE_TYPE);
            assertThat(envelope.get("eventType").asString()).isEqualTo(EVENT_TYPE);
            assertThat(envelope.get("eventVersion").asInt()).isEqualTo(EVENT_VERSION);
            assertThat(envelope.get("occurredAt").asString()).isNotBlank();
            assertThat(envelope.get("payload").get("taskId").asString()).isEqualTo(AGGREGATE_ID.toString());
        }

        @Test
        @DisplayName("wraps Jackson serialization failures in IllegalStateException")
        void shouldWrapSerializationFailure() {
            JacksonException failure = new StreamConstraintsException("serialization boom");
            doThrow(failure).when(objectMapper).writeValueAsString(any());

            assertThatThrownBy(() -> service.saveEvent(
                    AGGREGATE_ID,
                    AGGREGATE_TYPE,
                    EVENT_TYPE,
                    EVENT_VERSION,
                    Map.of("taskId", AGGREGATE_ID.toString())
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(ErrorMessages.OUTBOX_EVENT_SERIALIZATION_FAILED.formatted(EVENT_TYPE))
                    .hasCauseInstanceOf(StreamConstraintsException.class);

            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("propagates non-Jackson runtime failures unchanged")
        void shouldPropagateOtherRuntimeFailures() {
            IllegalStateException failure = new IllegalStateException("unexpected mapper failure");
            doThrow(failure).when(objectMapper).writeValueAsString(any());

            assertThatThrownBy(() -> service.saveEvent(
                    AGGREGATE_ID,
                    AGGREGATE_TYPE,
                    EVENT_TYPE,
                    EVENT_VERSION,
                    Map.of("taskId", AGGREGATE_ID.toString())
            ))
                    .isSameAs(failure);

            verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
        }
    }
}
