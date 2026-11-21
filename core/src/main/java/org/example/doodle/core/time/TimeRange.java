package org.example.doodle.core.time;

import org.example.doodle.core.DomainValidationException;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Time range {@code [startTime, endTime)}: the start is included, the end is not.
 * Both bounds are aligned to the {@link #QUANTUM}.
 */
public record TimeRange(LocalDateTime startTime, LocalDateTime endTime) {

    public static final Duration QUANTUM = Duration.ofMinutes(15);

    public TimeRange {
        if (startTime == null) {
            throw new DomainValidationException("startTime must not be null");
        }
        if (endTime == null) {
            throw new DomainValidationException("endTime must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new DomainValidationException("endTime must be after startTime");
        }
        requireAligned("startTime", startTime);
        requireAligned("endTime", endTime);
    }

    private static void requireAligned(String name, LocalDateTime time) {
        boolean aligned = time.getMinute() % QUANTUM.toMinutes() == 0
                && time.getSecond() == 0
                && time.getNano() == 0;
        if (!aligned) {
            throw new DomainValidationException(
                    name + " must be aligned to " + QUANTUM.toMinutes() + " minutes: " + time);
        }
    }
}
