package org.example.doodle.calendar.user;

import org.example.doodle.core.event.Event;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;

import java.time.LocalDateTime;

/**
 * Event of a user's log with the title of its meeting.
 *
 * @param meetingId    set only for {@link EventType#MEETING_BOOKED}
 * @param meetingTitle set only for {@link EventType#MEETING_BOOKED}
 */
public record EventRow(
        EventType eventType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long meetingId,
        String meetingTitle) {

    public Event toEvent() {
        return new Event(eventType, new TimeRange(startTime, endTime), meetingId);
    }
}
