package org.example.doodle.core.event;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventTest {

    private static final TimeRange RANGE = new TimeRange(
            LocalDateTime.of(2026, 11, 21, 10, 0),
            LocalDateTime.of(2026, 11, 21, 11, 0));

    @Test
    void rejectsMeetingBookedWithoutMeetingId() {
        assertThatThrownBy(() -> new Event(EventType.MEETING_BOOKED, RANGE, null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("meetingId");
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"MARKED_FREE", "MARKED_BUSY"})
    void rejectsMeetingIdForNonMeetingEvent(EventType eventType) {
        assertThatThrownBy(() -> new Event(eventType, RANGE, 42L))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("meetingId");
    }
}
