package org.example.doodle.core.projection;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectionIntervalTest {

    private static final TimeRange RANGE = new TimeRange(
            LocalDateTime.of(2026, 11, 21, 10, 0),
            LocalDateTime.of(2026, 11, 21, 11, 0));

    @Test
    void rejectsMeetingWithoutMeetingId() {
        assertThatThrownBy(() -> new ProjectionInterval(RANGE, TimeState.MEETING, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("meetingId");
    }

    @ParameterizedTest
    @EnumSource(value = TimeState.class, names = {"FREE", "BUSY"})
    void rejectsMeetingIdForNonMeetingState(TimeState state) {
        assertThatThrownBy(() -> new ProjectionInterval(RANGE, state, 42L))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("meetingId");
    }

    @Test
    void sameStateDependsOnStateAndMeeting() {
        ProjectionInterval free = new ProjectionInterval(RANGE, TimeState.FREE, null);
        ProjectionInterval busy = new ProjectionInterval(RANGE, TimeState.BUSY, null);
        ProjectionInterval meeting1 = new ProjectionInterval(RANGE, TimeState.MEETING, 1L);
        ProjectionInterval meeting2 = new ProjectionInterval(RANGE, TimeState.MEETING, 2L);

        assertThat(free.sameStateAs(new ProjectionInterval(RANGE, TimeState.FREE, null))).isTrue();
        assertThat(meeting1.sameStateAs(new ProjectionInterval(RANGE, TimeState.MEETING, 1L))).isTrue();
        assertThat(free.sameStateAs(busy)).isFalse();
        assertThat(meeting1.sameStateAs(meeting2)).isFalse();
    }
}
