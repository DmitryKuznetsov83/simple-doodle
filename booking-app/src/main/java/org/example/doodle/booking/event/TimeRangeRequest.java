package org.example.doodle.booking.event;

import jakarta.validation.constraints.NotNull;
import org.example.doodle.core.time.TimeRange;

import java.time.LocalDateTime;

public record TimeRangeRequest(@NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime) {

    /**
     * @throws org.example.doodle.core.DomainValidationException if the range is empty or not aligned
     */
    public TimeRange toTimeRange() {
        return new TimeRange(startTime, endTime);
    }
}
