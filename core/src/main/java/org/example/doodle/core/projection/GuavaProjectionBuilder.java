package org.example.doodle.core.projection;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.event.Event;
import org.example.doodle.core.time.TimeRange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * {@link ProjectionBuilder} on Guava {@link TreeRangeMap}: each event paints its state over the window,
 * {@code putCoalescing} overrides the painted range and merges it with adjacent ranges of the same paint.
 */
public final class GuavaProjectionBuilder implements ProjectionBuilder {

    private static final Paint UNCOVERED = new Paint(TimeState.BUSY, null);

    @Override
    public Projection build(TimeRange window, List<Event> events) {
        if (window == null) {
            throw new DomainValidationException("window must not be null");
        }
        if (events == null || events.stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("events must not be null or contain null");
        }

        Range<LocalDateTime> windowRange = toRange(window);
        RangeMap<LocalDateTime, Paint> painted = TreeRangeMap.create();
        painted.put(windowRange, UNCOVERED);

        for (Event event : events) {
            Range<LocalDateTime> eventRange = toRange(event.timeRange());
            if (!eventRange.isConnected(windowRange)) {
                continue;
            }
            Range<LocalDateTime> clipped = eventRange.intersection(windowRange);
            if (!clipped.isEmpty()) {
                painted.putCoalescing(clipped, new Paint(event.eventType().resultingState(), event.meetingId()));
            }
        }

        List<ProjectionInterval> intervals = painted.asMapOfRanges().entrySet().stream()
                .map(entry -> new ProjectionInterval(
                        toTimeRange(entry.getKey()), entry.getValue().state(), entry.getValue().meetingId()))
                .toList();

        return new Projection(window, intervals);
    }

    private static Range<LocalDateTime> toRange(TimeRange timeRange) {
        return Range.closedOpen(timeRange.startTime(), timeRange.endTime());
    }

    private static TimeRange toTimeRange(Range<LocalDateTime> range) {
        return new TimeRange(range.lowerEndpoint(), range.upperEndpoint());
    }

    /**
     * Paint of a time interval: different meetings are different paints, so they are never merged.
     */
    private record Paint(TimeState state, Long meetingId) {
    }
}
