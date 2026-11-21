package org.example.doodle.calendar.meeting;

import java.time.LocalDateTime;

/**
 * A meeting joined with one of its {@code MEETING_BOOKED} events: one row per participant.
 */
public record MeetingRow(
        long meetingId,
        String title,
        String description,
        long organizerId,
        String organizerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long participantId,
        String participantName) {
}
