package org.example.doodle.booking.event;

import org.example.doodle.core.event.EventType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Row of the append-only event log.
 *
 * @param creatorId who created the event, e.g. the meeting organizer
 * @param userId    whose calendar the event belongs to
 * @param meetingId set only for {@link EventType#MEETING_BOOKED}
 */
@Table("event")
public record EventEntity(
        @Id Long id,
        Long creatorId,
        Long userId,
        EventType eventType,
        Long meetingId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime createdAt) {
}
