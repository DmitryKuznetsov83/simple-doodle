package org.example.doodle.calendar;

import org.example.doodle.core.event.EventType;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;

/**
 * Inserts rows for repository tests.
 */
public record TestData(JdbcClient jdbc) {

    public long user(String name) {
        return jdbc.sql("INSERT INTO users (name) VALUES (?) RETURNING id")
                .param(name)
                .query(Long.class)
                .single();
    }

    public long meeting(String title, String description) {
        return jdbc.sql("INSERT INTO meeting (title, description) VALUES (?, ?) RETURNING id")
                .params(title, description)
                .query(Long.class)
                .single();
    }

    public long event(long creatorId, long userId, EventType eventType, Long meetingId,
                      LocalDateTime startTime, LocalDateTime endTime) {
        return jdbc.sql("""
                        INSERT INTO event (creator_id, user_id, event_type, meeting_id, start_time, end_time)
                        VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id""")
                .params(creatorId, userId, eventType.name(), meetingId, startTime, endTime)
                .query(Long.class)
                .single();
    }
}
