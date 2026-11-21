package org.example.doodle.core.projection;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.time.TimeRange;

import java.util.Objects;

/**
 * Interval of a projection with its resulting time state.
 *
 * @param meetingId set only for {@link TimeState#MEETING}
 */
public record ProjectionInterval(TimeRange timeRange, TimeState state, Long meetingId) {

    public ProjectionInterval {
        if (timeRange == null) {
            throw new DomainValidationException("timeRange must not be null");
        }
        if (state == null) {
            throw new DomainValidationException("state must not be null");
        }
        if ((state == TimeState.MEETING) != (meetingId != null)) {
            throw new DomainValidationException("meetingId must be set only for " + TimeState.MEETING);
        }
    }

    /**
     * Whether the two intervals have the same state (and the same meeting) and would be merged if adjacent.
     */
    public boolean sameStateAs(ProjectionInterval other) {
        return state == other.state && Objects.equals(meetingId, other.meetingId);
    }
}
