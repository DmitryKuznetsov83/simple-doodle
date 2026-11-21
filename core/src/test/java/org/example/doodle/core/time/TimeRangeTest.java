package org.example.doodle.core.time;

import org.example.doodle.core.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    private static final LocalDateTime T_10_00 = LocalDateTime.of(2026, 11, 21, 10, 0);

    @Test
    void createsRangeOfOneQuantum() {
        TimeRange range = new TimeRange(T_10_00, T_10_00.plus(TimeRange.QUANTUM));

        assertThat(range.endTime()).isEqualTo(LocalDateTime.of(2026, 11, 21, 10, 15));
    }

    @Test
    void rejectsEmptyRange() {
        assertThatThrownBy(() -> new TimeRange(T_10_00, T_10_00))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> new TimeRange(T_10_00, T_10_00.minusMinutes(15)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void rejectsStartTimeNotAlignedToQuantum() {
        assertThatThrownBy(() -> new TimeRange(T_10_00.withMinute(14), T_10_00.plusHours(1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("startTime must be aligned");
    }

    @Test
    void rejectsEndTimeNotAlignedToQuantum() {
        assertThatThrownBy(() -> new TimeRange(T_10_00, T_10_00.plusHours(1).withMinute(14)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("endTime must be aligned");
    }

    @Test
    void rejectsSecondsAndNanosEvenIfMinutesAreAligned() {
        assertThatThrownBy(() -> new TimeRange(T_10_00.withSecond(1), T_10_00.plusHours(1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("startTime must be aligned");
        assertThatThrownBy(() -> new TimeRange(T_10_00, T_10_00.plusHours(1).withNano(1)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("endTime must be aligned");
    }
}
