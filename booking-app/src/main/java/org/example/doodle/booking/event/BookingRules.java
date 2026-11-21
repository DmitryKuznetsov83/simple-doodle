package org.example.doodle.booking.event;

import org.example.doodle.core.time.TimeRange;

import java.time.LocalDateTime;

/**
 * Booking rules shared by all commands that append events.
 */
public final class BookingRules {

    private BookingRules() {
    }

    /**
     * @throws StartTimeInPastException if the range does not start strictly after {@code now}
     */
    public static void requireStartInFuture(TimeRange timeRange, LocalDateTime now) {
        if (!timeRange.startTime().isAfter(now)) {
            throw new StartTimeInPastException(timeRange.startTime());
        }
    }
}
