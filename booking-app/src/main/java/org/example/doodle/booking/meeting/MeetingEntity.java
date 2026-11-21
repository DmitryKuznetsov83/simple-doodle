package org.example.doodle.booking.meeting;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("meeting")
public record MeetingEntity(@Id Long id, String title, String description) {
}
