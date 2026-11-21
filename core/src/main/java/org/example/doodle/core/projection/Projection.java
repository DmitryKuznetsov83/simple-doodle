package org.example.doodle.core.projection;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.time.TimeRange;

import java.util.List;
import java.util.Objects;

/**
 * A user's calendar within a window: the result of applying the event log.
 * <p>
 * Invariants:
 * <ul>
 *     <li>intervals are sorted, adjacent to each other and cover the whole window</li>
 *     <li>adjacent intervals have different states (or different meetings), i.e. they are merged</li>
 * </ul>
 */
public record Projection(TimeRange window, List<ProjectionInterval> intervals) {

    public Projection {
        if (window == null) {
            throw new DomainValidationException("window must not be null");
        }
        if (intervals == null || intervals.isEmpty()) {
            throw new DomainValidationException("intervals must cover the window");
        }
        if (intervals.stream().anyMatch(Objects::isNull)) {
            throw new DomainValidationException("intervals must not contain null");
        }
        intervals = List.copyOf(intervals);

        if (!intervals.getFirst().timeRange().startTime().equals(window.startTime())) {
            throw new DomainValidationException("first interval must start at the window start");
        }
        if (!intervals.getLast().timeRange().endTime().equals(window.endTime())) {
            throw new DomainValidationException("last interval must end at the window end");
        }
        for (int i = 1; i < intervals.size(); i++) {
            ProjectionInterval previous = intervals.get(i - 1);
            ProjectionInterval current = intervals.get(i);
            if (!current.timeRange().startTime().equals(previous.timeRange().endTime())) {
                throw new DomainValidationException(
                        "intervals must be adjacent: " + previous.timeRange() + ", " + current.timeRange());
            }
            if (current.sameStateAs(previous)) {
                throw new DomainValidationException(
                        "adjacent intervals with the same state must be merged: "
                                + previous.timeRange() + ", " + current.timeRange());
            }
        }
    }
}
