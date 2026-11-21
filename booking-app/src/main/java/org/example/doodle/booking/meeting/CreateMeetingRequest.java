package org.example.doodle.booking.meeting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.doodle.core.time.TimeRange;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param participantIds other participants; the organizer is added automatically,
 *                       duplicates are collapsed, may be empty
 */
public record CreateMeetingRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull List<@NotNull Long> participantIds,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime) {

    /**
     * @throws org.example.doodle.core.DomainValidationException if the range is empty or not aligned
     */
    public TimeRange toTimeRange() {
        return new TimeRange(startTime, endTime);
    }
}
