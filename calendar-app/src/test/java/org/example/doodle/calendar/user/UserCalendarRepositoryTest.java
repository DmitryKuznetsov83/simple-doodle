package org.example.doodle.calendar.user;

import org.example.doodle.calendar.RepositoryTest;
import org.example.doodle.calendar.TestData;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RepositoryTest
class UserCalendarRepositoryTest {

    private static final TimeRange WINDOW = new TimeRange(at(10, 0), at(12, 0));

    @Autowired
    private UserCalendarRepository repository;

    @Autowired
    private JdbcClient jdbc;

    private TestData data;
    private long john;
    private long paul;

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc);
        john = data.user("John");
        paul = data.user("Paul");
    }

    @Test
    void findsEventsOverlappingHalfOpenWindow() {
        // touch the window bounds, do not overlap
        free(john, at(8, 0), at(10, 0));
        free(john, at(12, 0), at(13, 0));
        // overlap
        free(john, at(9, 45), at(10, 15));
        free(john, at(11, 45), at(12, 15));
        free(john, at(8, 0), at(14, 0));
        free(john, at(10, 30), at(11, 0));
        // another user
        free(paul, at(10, 0), at(12, 0));

        assertThat(repository.findEvents(john, WINDOW))
                .extracting(EventRow::startTime, EventRow::endTime)
                .containsExactly(
                        tuple(at(9, 45), at(10, 15)),
                        tuple(at(11, 45), at(12, 15)),
                        tuple(at(8, 0), at(14, 0)),
                        tuple(at(10, 30), at(11, 0)));
    }

    @Test
    void findsEventsInLogOrderNotByStartTime() {
        data.event(john, john, EventType.MARKED_FREE, null, at(11, 0), at(12, 0));
        data.event(john, john, EventType.MARKED_BUSY, null, at(10, 0), at(11, 0));
        data.event(john, john, EventType.MARKED_FREE, null, at(10, 30), at(11, 30));

        assertThat(repository.findEvents(john, WINDOW))
                .extracting(EventRow::eventType, EventRow::startTime)
                .containsExactly(
                        tuple(EventType.MARKED_FREE, at(11, 0)),
                        tuple(EventType.MARKED_BUSY, at(10, 0)),
                        tuple(EventType.MARKED_FREE, at(10, 30)));
    }

    @Test
    void findsMeetingTitleOfMeetingEvents() {
        long meeting = data.meeting("Sync", "Weekly");
        free(john, at(10, 0), at(12, 0));
        data.event(paul, john, EventType.MEETING_BOOKED, meeting, at(11, 0), at(12, 0));

        assertThat(repository.findEvents(john, WINDOW)).containsExactly(
                new EventRow(EventType.MARKED_FREE, at(10, 0), at(12, 0), null, null),
                new EventRow(EventType.MEETING_BOOKED, at(11, 0), at(12, 0), meeting, "Sync"));
    }

    private void free(long userId, LocalDateTime startTime, LocalDateTime endTime) {
        data.event(userId, userId, EventType.MARKED_FREE, null, startTime, endTime);
    }

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 22, hour, minute);
    }
}
