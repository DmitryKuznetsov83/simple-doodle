package org.example.doodle.booking.meeting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param participantIds all participants, the organizer first
 */
public record MeetingResponse(
        long meetingId,
        long organizerId,
        String title,
        String description,
        List<Long> participantIds,
        LocalDateTime startTime,
        LocalDateTime endTime) {
}
