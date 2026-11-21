package org.example.doodle.core.event;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.time.TimeRange;

/**
 * Event of a user's event log: what the projection needs to know about it.
 *
 * @param meetingId set only for {@link EventType#MEETING_BOOKED}
 */
public record Event(EventType eventType, TimeRange timeRange, Long meetingId) {

    public Event {
        if (eventType == null) {
            throw new DomainValidationException("eventType must not be null");
        }
        if (timeRange == null) {
            throw new DomainValidationException("timeRange must not be null");
        }
        if ((eventType == EventType.MEETING_BOOKED) == (meetingId == null)) {
            throw new DomainValidationException("meetingId must be set only for " + EventType.MEETING_BOOKED);
        }
    }
}
