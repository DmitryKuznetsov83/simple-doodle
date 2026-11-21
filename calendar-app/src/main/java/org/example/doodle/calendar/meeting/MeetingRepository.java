package org.example.doodle.calendar.meeting;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MeetingRepository {

    private final JdbcClient jdbc;

    public MeetingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * The meeting with its events, one row per participant, in log order.
     * Empty if the meeting does not exist or has no events.
     */
    public List<MeetingRow> findMeeting(long meetingId) {
        return jdbc.sql("""
                        SELECT m.id AS meeting_id, m.title, m.description,
                               e.creator_id AS organizer_id, o.name AS organizer_name,
                               e.start_time, e.end_time,
                               e.user_id AS participant_id, p.name AS participant_name
                        FROM meeting m
                        JOIN event e ON e.meeting_id = m.id
                        JOIN users o ON o.id = e.creator_id
                        JOIN users p ON p.id = e.user_id
                        WHERE m.id = :meetingId
                        ORDER BY e.id
                        """)
                .param("meetingId", meetingId)
                .query(MeetingRow.class)
                .list();
    }
}
