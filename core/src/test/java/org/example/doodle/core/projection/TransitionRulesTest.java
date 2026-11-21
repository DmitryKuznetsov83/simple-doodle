package org.example.doodle.core.projection;

import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransitionRulesTest {

    private static final TimeRange WINDOW = range(10, 0, 11, 0);

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"MARKED_FREE", "MARKED_BUSY"})
    void markAllowedWithoutMeetings(EventType eventType) {
        Projection projection = projection(
                interval(10, 0, 10, 30, TimeState.FREE, null),
                interval(10, 30, 11, 0, TimeState.BUSY, null));

        assertThat(TransitionRules.allows(eventType, projection)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"MARKED_FREE", "MARKED_BUSY"})
    void markRejectedOnPartialOverlapWithMeeting(EventType eventType) {
        Projection projection = projection(
                interval(10, 0, 10, 45, TimeState.FREE, null),
                interval(10, 45, 11, 0, TimeState.MEETING, 1L));

        assertThat(TransitionRules.allows(eventType, projection)).isFalse();
    }

    @Test
    void meetingAllowedOnEntirelyFreeWindow() {
        Projection projection = projection(interval(10, 0, 11, 0, TimeState.FREE, null));

        assertThat(TransitionRules.allows(EventType.MEETING_BOOKED, projection)).isTrue();
    }

    @Test
    void meetingRejectedOnPartiallyBusyWindow() {
        Projection projection = projection(
                interval(10, 0, 10, 45, TimeState.FREE, null),
                interval(10, 45, 11, 0, TimeState.BUSY, null));

        assertThat(TransitionRules.allows(EventType.MEETING_BOOKED, projection)).isFalse();
    }

    @Test
    void meetingRejectedOverAnotherMeeting() {
        Projection projection = projection(
                interval(10, 0, 10, 15, TimeState.FREE, null),
                interval(10, 15, 11, 0, TimeState.MEETING, 1L));

        assertThat(TransitionRules.allows(EventType.MEETING_BOOKED, projection)).isFalse();
    }

    private static Projection projection(ProjectionInterval... intervals) {
        return new Projection(WINDOW, List.of(intervals));
    }

    private static ProjectionInterval interval(int startHour, int startMinute, int endHour, int endMinute,
                                               TimeState state, Long meetingId) {
        return new ProjectionInterval(range(startHour, startMinute, endHour, endMinute), state, meetingId);
    }

    private static TimeRange range(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeRange(
                LocalDateTime.of(2026, 11, 22, startHour, startMinute),
                LocalDateTime.of(2026, 11, 22, endHour, endMinute));
    }
}
