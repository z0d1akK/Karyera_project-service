package com.zodiakk.projectservice.outbox.dto;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEventEnvelope(
        UUID eventId,
        UUID aggregateId,
        String eventType,
        String aggregateType,
        int eventVersion,
        OffsetDateTime occurredAt,
        JsonNode payload
) {
}
