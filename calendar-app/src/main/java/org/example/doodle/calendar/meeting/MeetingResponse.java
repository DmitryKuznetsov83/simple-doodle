package org.example.doodle.calendar.meeting;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.doodle.calendar.user.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param participants all participants, the organizer first
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeetingResponse(
        long meetingId,
        String title,
        String description,
        User organizer,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<User> participants) {
}
