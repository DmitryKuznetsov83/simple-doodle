package org.example.doodle.calendar.meeting;

import org.example.doodle.calendar.RepositoryTest;
import org.example.doodle.calendar.TestData;
import org.example.doodle.core.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
class MeetingRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 22, 11, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired
    private MeetingRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private TestData data;
    private long john;
    private long paul;
    private long george;

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc);
        john = data.user("John");
        paul = data.user("Paul");
        george = data.user("George");
    }

    @Test
    void findsMeetingWithParticipantsInLogOrder() {
        long meeting = data.meeting("Sync", "Weekly");
        // organizer Paul first, then participants not in id order
        booked(paul, paul, meeting);
        booked(paul, george, meeting);
        booked(paul, john, meeting);

        assertThat(repository.findMeeting(meeting)).containsExactly(
                new MeetingRow(meeting, "Sync", "Weekly", paul, "Paul", START, END, paul, "Paul"),
                new MeetingRow(meeting, "Sync", "Weekly", paul, "Paul", START, END, george, "George"),
                new MeetingRow(meeting, "Sync", "Weekly", paul, "Paul", START, END, john, "John"));
    }

    @Test
    void findsOnlyEventsOfTheMeeting() {
        long meeting = data.meeting("Sync", null);
        long other = data.meeting("Retro", null);
        data.event(john, john, EventType.MARKED_FREE, null, START, END);
        booked(john, john, meeting);
        booked(paul, paul, other);
        booked(paul, john, other);

        assertThat(repository.findMeeting(meeting)).containsExactly(
                new MeetingRow(meeting, "Sync", null, john, "John", START, END, john, "John"));
    }

    @Test
    void findsNothingForUnknownMeeting() {
        assertThat(repository.findMeeting(-1)).isEmpty();
    }

    @Test
    void findsNothingForMeetingWithoutEvents() {
        long meeting = data.meeting("Sync", "Weekly");

        assertThat(repository.findMeeting(meeting)).isEmpty();
    }

    private void booked(long organizerId, long participantId, long meetingId) {
        data.event(organizerId, participantId, EventType.MEETING_BOOKED, meetingId, START, END);
    }
}
