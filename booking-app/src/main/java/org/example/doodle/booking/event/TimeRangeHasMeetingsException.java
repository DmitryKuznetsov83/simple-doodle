package org.example.doodle.booking.event;

import org.example.doodle.core.time.TimeRange;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

/**
 * Meetings cannot be overwritten by marking time free or busy.
 */
public class TimeRangeHasMeetingsException extends ResponseStatusException {

    public TimeRangeHasMeetingsException(TimeRange timeRange, Collection<Long> meetingIds) {
        super(HttpStatus.CONFLICT, "Time range [" + timeRange.startTime() + ", " + timeRange.endTime()
                + ") overlaps meetings " + meetingIds);
    }
}
