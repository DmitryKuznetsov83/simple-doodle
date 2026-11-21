package org.example.doodle.core.projection;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectionTest {

    private static final TimeRange WINDOW = range(9, 0, 13, 0);

    @Test
    void createsProjectionFromSpecExample() {
        List<ProjectionInterval> intervals = List.of(
                interval(9, 0, 10, 0, TimeState.FREE, null),
                interval(10, 0, 10, 30, TimeState.BUSY, null),
                interval(10, 30, 11, 0, TimeState.FREE, null),
                interval(11, 0, 12, 0, TimeState.MEETING, 1L),
                interval(12, 0, 13, 0, TimeState.BUSY, null));

        Projection projection = new Projection(WINDOW, intervals);

        assertThat(projection.intervals()).containsExactlyElementsOf(intervals);
    }

    @Test
    void allowsAdjacentDifferentMeetings() {
        Projection projection = new Projection(WINDOW, List.of(
                interval(9, 0, 11, 0, TimeState.MEETING, 1L),
                interval(11, 0, 13, 0, TimeState.MEETING, 2L)));

        assertThat(projection.intervals()).hasSize(2);
    }

    @Test
    void rejectsGapAtWindowStart() {
        assertThatThrownBy(() -> new Projection(WINDOW, List.of(interval(9, 15, 13, 0, TimeState.BUSY, null))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("window start");
    }

    @Test
    void rejectsGapAtWindowEnd() {
        assertThatThrownBy(() -> new Projection(WINDOW, List.of(interval(9, 0, 12, 45, TimeState.BUSY, null))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("window end");
    }

    @Test
    void rejectsGapBetweenIntervals() {
        assertThatThrownBy(() -> new Projection(WINDOW, List.of(
                interval(9, 0, 10, 0, TimeState.FREE, null),
                interval(10, 15, 13, 0, TimeState.BUSY, null))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("adjacent");
    }

    @Test
    void rejectsOverlappingIntervals() {
        assertThatThrownBy(() -> new Projection(WINDOW, List.of(
                interval(9, 0, 10, 15, TimeState.FREE, null),
                interval(10, 0, 13, 0, TimeState.BUSY, null))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("adjacent");
    }

    @Test
    void rejectsNotMergedIntervals() {
        assertThatThrownBy(() -> new Projection(WINDOW, List.of(
                interval(9, 0, 11, 0, TimeState.FREE, null),
                interval(11, 0, 13, 0, TimeState.FREE, null))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("merged");
    }

    @Test
    void rejectsNotMergedIntervalsOfSameMeeting() {
        assertThatThrownBy(() -> new Projection(WINDOW, List.of(
                interval(9, 0, 11, 0, TimeState.MEETING, 1L),
                interval(11, 0, 13, 0, TimeState.MEETING, 1L))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("merged");
    }

    private static ProjectionInterval interval(int startHour, int startMinute, int endHour, int endMinute,
                                               TimeState state, Long meetingId) {
        return new ProjectionInterval(range(startHour, startMinute, endHour, endMinute), state, meetingId);
    }

    private static TimeRange range(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeRange(
                LocalDateTime.of(2026, 11, 21, startHour, startMinute),
                LocalDateTime.of(2026, 11, 21, endHour, endMinute));
    }
}
