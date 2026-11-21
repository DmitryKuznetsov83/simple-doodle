package org.example.doodle.calendar.user;

import org.example.doodle.core.time.TimeRange;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserCalendarRepository {

    private final JdbcClient jdbc;

    public UserCalendarRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findUser(long userId) {
        return jdbc.sql("SELECT id, name FROM users WHERE id = :userId")
                .param("userId", userId)
                .query(User.class)
                .optional();
    }

    /**
     * Events of the user overlapping the window, in log order.
     */
    public List<EventRow> findEvents(long userId, TimeRange window) {
        return jdbc.sql("""
                        SELECT e.event_type, e.start_time, e.end_time, e.meeting_id, m.title AS meeting_title
                        FROM event e
                        LEFT JOIN meeting m ON m.id = e.meeting_id
                        WHERE e.user_id = :userId
                          AND e.start_time < :endTime
                          AND e.end_time > :startTime
                        ORDER BY e.id
                        """)
                .param("userId", userId)
                .param("startTime", window.startTime())
                .param("endTime", window.endTime())
                .query(EventRow.class)
                .list();
    }
}
