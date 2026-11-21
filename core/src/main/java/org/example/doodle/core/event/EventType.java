package org.example.doodle.core.event;

import org.example.doodle.core.projection.TimeState;

/**
 * Type of an event in the event log. Each event sets its {@link #resultingState()} on its time range.
 */
public enum EventType {
    MARKED_FREE(TimeState.FREE),
    MARKED_BUSY(TimeState.BUSY),
    MEETING_BOOKED(TimeState.MEETING);

    private final TimeState resultingState;

    EventType(TimeState resultingState) {
        this.resultingState = resultingState;
    }

    public TimeState resultingState() {
        return resultingState;
    }
}
