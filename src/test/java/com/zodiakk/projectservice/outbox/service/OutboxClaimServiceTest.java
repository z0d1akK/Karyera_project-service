package com.zodiakk.projectservice.outbox.service;

import com.zodiakk.projectservice.outbox.config.OutboxProperties;
import com.zodiakk.projectservice.outbox.entity.OutboxEvent;
import com.zodiakk.projectservice.outbox.entity.OutboxEventStatus;
import com.zodiakk.projectservice.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxClaimServiceTest {

    private static final int BATCH_SIZE = 10;

    private static final long LEASE_SECONDS = 180L;

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final UUID AGGREGATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private OutboxEventRepository repository;

    private OutboxClaimService service;

    @BeforeEach
    void setUp() {
        OutboxProperties properties = new OutboxProperties(BATCH_SIZE, 2_000L, LEASE_SECONDS, 15L);
        service = new OutboxClaimService(repository, properties);
    }

    @Nested
    @DisplayName("claimBatch")
    class ClaimBatch {

        @Test
        @DisplayName("claims the repository batch, marks events PROCESSING, bumps attempts and leases them")
        void shouldClaimBatch() {
            OutboxEvent first = event(0);
            OutboxEvent second = event(2);

            ArgumentCaptor<OffsetDateTime> nowCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            when(repository.findAvailableForUpdate(nowCaptor.capture(), eq(BATCH_SIZE)))
                    .thenReturn(List.of(first, second));

            List<OutboxEvent> claimed = service.claimBatch();

            assertThat(claimed).containsExactly(first, second);

            OffsetDateTime claimTime = nowCaptor.getValue();
            assertThat(claimTime).isNotNull();
            assertThat(claimTime.getOffset()).isEqualTo(ZoneOffset.UTC);

            assertThat(first.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(first.getAttempts()).isEqualTo(1);
            assertThat(first.getLockedUntil()).isEqualTo(claimTime.plusSeconds(LEASE_SECONDS));

            assertThat(second.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(second.getAttempts()).isEqualTo(3);
            assertThat(second.getLockedUntil()).isEqualTo(claimTime.plusSeconds(LEASE_SECONDS));

            verify(repository).findAvailableForUpdate(any(OffsetDateTime.class), eq(BATCH_SIZE));
            verify(repository).flush();
        }

        @Test
        @DisplayName("uses the configured batch size as the repository limit")
        void shouldUseConfiguredBatchSize() {
            ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
            when(repository.findAvailableForUpdate(any(OffsetDateTime.class), limitCaptor.capture()))
                    .thenReturn(List.of());

            service.claimBatch();

            assertThat(limitCaptor.getValue()).isEqualTo(BATCH_SIZE);
            verify(repository).flush();
        }

        @Test
        @DisplayName("returns an immutable snapshot of the claimed events")
        void shouldReturnImmutableSnapshot() {
            OutboxEvent claimedEvent = event(0);
            List<OutboxEvent> mutableBatch = new ArrayList<>();
            mutableBatch.add(claimedEvent);

            when(repository.findAvailableForUpdate(any(OffsetDateTime.class), eq(BATCH_SIZE)))
                    .thenReturn(mutableBatch);

            List<OutboxEvent> claimed = service.claimBatch();

            assertThat(claimed).containsExactly(claimedEvent);
            assertThatThrownBy(() -> claimed.add(event(1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("flushes even when there is nothing to claim")
        void shouldFlushEmptyBatch() {
            when(repository.findAvailableForUpdate(any(OffsetDateTime.class), eq(BATCH_SIZE)))
                    .thenReturn(List.of());

            assertThat(service.claimBatch()).isEmpty();

            verify(repository).flush();
        }

        @Test
        @DisplayName("propagates repository failures")
        void shouldPropagateRepositoryFailure() {
            when(repository.findAvailableForUpdate(any(OffsetDateTime.class), eq(BATCH_SIZE)))
                    .thenThrow(new IllegalStateException("lock timeout"));

            assertThatThrownBy(() -> service.claimBatch())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("lock timeout");
        }
    }

    @Nested
    @DisplayName("markPublished")
    class MarkPublished {

        @Test
        @DisplayName("returns true when the repository updates exactly one row")
        void shouldMarkPublished() {
            ArgumentCaptor<OffsetDateTime> publishedAtCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            when(repository.markPublished(
                    eq(EVENT_ID),
                    eq(3),
                    eq(OutboxEventStatus.PROCESSING),
                    eq(OutboxEventStatus.PUBLISHED),
                    publishedAtCaptor.capture()
            )).thenReturn(1);

            OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
            boolean updated = service.markPublished(EVENT_ID, 3);

            assertThat(updated).isTrue();
            assertThat(publishedAtCaptor.getValue())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(OffsetDateTime.now(ZoneOffset.UTC));
            assertThat(publishedAtCaptor.getValue().getOffset()).isEqualTo(ZoneOffset.UTC);
        }

        @Test
        @DisplayName("returns false when no row was updated because the claim expired")
        void shouldReportExpiredClaim() {
            when(repository.markPublished(any(), eq(3), any(), any(), any())).thenReturn(0);

            assertThat(service.markPublished(EVENT_ID, 3)).isFalse();
        }
    }

    @Nested
    @DisplayName("markFailed")
    class MarkFailed {

        @Test
        @DisplayName("schedules an exponential retry and returns true when a row is updated")
        void shouldScheduleRetry() {
            ArgumentCaptor<OffsetDateTime> nowCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            ArgumentCaptor<OffsetDateTime> nextAttemptCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

            when(repository.markFailed(
                    eq(EVENT_ID),
                    eq(1),
                    eq(OutboxEventStatus.PROCESSING),
                    eq(OutboxEventStatus.PENDING),
                    nowCaptor.capture(),
                    nextAttemptCaptor.capture()
            )).thenReturn(1);

            boolean updated = service.markFailed(EVENT_ID, 1);

            assertThat(updated).isTrue();
            assertThat(nextAttemptCaptor.getValue())
                    .isEqualTo(nowCaptor.getValue().plusSeconds(2));
            assertThat(nowCaptor.getValue().getOffset()).isEqualTo(ZoneOffset.UTC);
        }

        @Test
        @DisplayName("caps the retry exponent at eight")
        void shouldCapRetryDelay() {
            ArgumentCaptor<OffsetDateTime> nowCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            ArgumentCaptor<OffsetDateTime> nextAttemptCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

            when(repository.markFailed(
                    eq(EVENT_ID),
                    eq(20),
                    eq(OutboxEventStatus.PROCESSING),
                    eq(OutboxEventStatus.PENDING),
                    nowCaptor.capture(),
                    nextAttemptCaptor.capture()
            )).thenReturn(1);

            service.markFailed(EVENT_ID, 20);

            assertThat(nextAttemptCaptor.getValue())
                    .isEqualTo(nowCaptor.getValue().plusSeconds(256));
        }

        @Test
        @DisplayName("returns false when the claim has been lost")
        void shouldReportLostClaim() {
            when(repository.markFailed(any(), eq(2), any(), any(), any(), any())).thenReturn(0);

            assertThat(service.markFailed(EVENT_ID, 2)).isFalse();
        }
    }

    private OutboxEvent event(int attempts) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return OutboxEvent.builder()
                .id(EVENT_ID)
                .aggregateId(AGGREGATE_ID)
                .aggregateType("ProjectTask")
                .eventType("ProjectTaskCompleted")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxEventStatus.PENDING)
                .attempts(attempts)
                .createdAt(now)
                .nextAttemptAt(now)
                .build();
    }
}
