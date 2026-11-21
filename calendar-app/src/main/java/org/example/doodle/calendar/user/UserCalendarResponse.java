package org.example.doodle.calendar.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.doodle.core.projection.TimeState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A user's calendar within the window {@code [startTime, endTime)}.
 */
public record UserCalendarResponse(
        User user,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<Interval> intervals) {

    /**
     * @param meetingId    set only for {@link TimeState#MEETING}
     * @param meetingTitle set only for {@link TimeState#MEETING}
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Interval(
            LocalDateTime startTime,
            LocalDateTime endTime,
            TimeState state,
            Long meetingId,
            String meetingTitle) {
    }
}
