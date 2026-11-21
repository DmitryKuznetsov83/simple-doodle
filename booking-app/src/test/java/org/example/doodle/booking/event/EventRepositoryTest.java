package org.example.doodle.booking.event;

import org.example.doodle.booking.PostgresTestcontainer;
import org.example.doodle.booking.meeting.MeetingEntity;
import org.example.doodle.booking.meeting.MeetingRepository;
import org.example.doodle.booking.user.User;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainer.class)
class EventRepositoryTest {

    private static final LocalDateTime START = at(10, 0);
    private static final LocalDateTime END = at(11, 0);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    private long alice;
    private long bob;
    private long carol;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User(null, "alice")).id();
        bob = userRepository.save(new User(null, "bob")).id();
        carol = userRepository.save(new User(null, "carol")).id();
    }

    @Test
    void findsEventsOfRequestedUsersOverlappingRangeInLogOrder() {
        long meetingId = meetingRepository.save(new MeetingEntity(null, "Sync", null)).id();

        EventEntity aliceLater = save(alice, EventType.MARKED_FREE, null, at(10, 30), at(12, 0));
        EventEntity bobCovering = save(bob, EventType.MARKED_FREE, null, at(9, 0), at(12, 0));
        EventEntity aliceEarlierStart = save(alice, EventType.MARKED_BUSY, null, at(9, 45), at(10, 15));
        EventEntity bobMeeting = save(bob, EventType.MEETING_BOOKED, meetingId, at(10, 45), at(11, 0));

        // adjacent to the range: [start, end) do not overlap
        save(alice, EventType.MARKED_FREE, null, at(9, 0), START);
        save(bob, EventType.MARKED_FREE, null, END, at(12, 0));
        // another user
        save(carol, EventType.MARKED_FREE, null, START, END);

        List<EventEntity> events = eventRepository.findOverlapping(List.of(alice, bob), START, END);

        assertThat(events).containsExactly(aliceLater, bobCovering, aliceEarlierStart, bobMeeting);
    }

    private EventEntity save(long userId, EventType eventType, Long meetingId,
                             LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.save(new EventEntity(
                null, userId, userId, eventType, meetingId, startTime, endTime, at(8, 0)));
    }

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 22, hour, minute);
    }
}
