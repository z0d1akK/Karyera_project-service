package com.zodiakk.projectservice.housekeeping;

import com.zodiakk.projectservice.integration.repository.ProcessedEventRepository;
import com.zodiakk.projectservice.outbox.entity.OutboxEventStatus;
import com.zodiakk.projectservice.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceTest {

    private static final long OUTBOX_RETENTION_DAYS = 7L;

    private static final long PROCESSED_EVENTS_RETENTION_DAYS = 30L;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private HousekeepingService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "outboxRetentionDays", OUTBOX_RETENTION_DAYS);
        ReflectionTestUtils.setField(
                service,
                "processedEventsRetentionDays",
                PROCESSED_EVENTS_RETENTION_DAYS
        );
    }

    @Nested
    @DisplayName("cleanup")
    class Cleanup {

        @Test
        @DisplayName("deletes published outbox events and processed events older than their retention windows")
        void shouldCleanupExpiredRecords() {
            when(outboxEventRepository.deletePublishedBefore(eq(OutboxEventStatus.PUBLISHED), any()))
                    .thenReturn(3);
            when(processedEventRepository.deleteProcessedBefore(any())).thenReturn(5);

            OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
            service.cleanup();

            ArgumentCaptor<OffsetDateTime> outboxCutoff =
                    ArgumentCaptor.forClass(OffsetDateTime.class);
            ArgumentCaptor<OffsetDateTime> processedCutoff =
                    ArgumentCaptor.forClass(OffsetDateTime.class);

            verify(outboxEventRepository).deletePublishedBefore(
                    eq(OutboxEventStatus.PUBLISHED),
                    outboxCutoff.capture()
            );
            verify(processedEventRepository).deleteProcessedBefore(processedCutoff.capture());

            assertThatCutoffIsCloseTo(
                    outboxCutoff.getValue(),
                    before.minusDays(OUTBOX_RETENTION_DAYS)
            );
            assertThatCutoffIsCloseTo(
                    processedCutoff.getValue(),
                    before.minusDays(PROCESSED_EVENTS_RETENTION_DAYS)
            );
            assertThat(outboxCutoff.getValue().getOffset()).isEqualTo(ZoneOffset.UTC);
            assertThat(processedCutoff.getValue().getOffset()).isEqualTo(ZoneOffset.UTC);
        }

        @Test
        @DisplayName("tolerates a cleanup that deletes nothing")
        void shouldTolerateEmptyCleanup() {
            when(outboxEventRepository.deletePublishedBefore(any(), any())).thenReturn(0);
            when(processedEventRepository.deleteProcessedBefore(any())).thenReturn(0);

            assertThatCode(() -> service.cleanup()).doesNotThrowAnyException();

            verify(outboxEventRepository).deletePublishedBefore(
                    eq(OutboxEventStatus.PUBLISHED),
                    any()
            );
            verify(processedEventRepository).deleteProcessedBefore(any());
        }

        @Test
        @DisplayName("propagates repository failures")
        void shouldPropagateRepositoryFailure() {
            when(outboxEventRepository.deletePublishedBefore(any(), any()))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThatThrownBy(() -> service.cleanup())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("database unavailable");
        }
    }

    private void assertThatCutoffIsCloseTo(OffsetDateTime actual, OffsetDateTime expected) {
        long difference = Math.abs(
                actual.toInstant().toEpochMilli() - expected.toInstant().toEpochMilli()
        );

        assertThat(difference).isLessThan(5_000L);
    }
}
