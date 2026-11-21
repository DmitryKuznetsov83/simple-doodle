package org.example.doodle.booking;

import org.example.doodle.booking.event.EventEntity;
import org.example.doodle.booking.event.EventRepository;
import org.example.doodle.booking.event.EventService;
import org.example.doodle.booking.event.TimeRangeHasMeetingsException;
import org.example.doodle.booking.meeting.MeetingService;
import org.example.doodle.booking.meeting.ParticipantsNotFreeException;
import org.example.doodle.booking.user.User;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parallel commands on the same users, with real commits. Every test creates its own users.
 */
@SpringBootTest
@Import(PostgresTestcontainer.class)
class ConcurrentBookingTest {

    private static final int THREADS = 8;
    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void parallelMeetingsOnSameSlotBookExactlyOne() throws Exception {
        long alice = newUser();
        long bob = newUser();
        long carol = newUser();
        markFree(List.of(alice, bob, carol), range(9, 12));
        TimeRange slot = range(10, 11);

        // the same participants, listed in different orders: locking must not deadlock
        List<List<Long>> orders = List.of(List.of(alice, bob, carol), List.of(carol, bob, alice),
                List.of(bob, alice, carol), List.of(carol, alice, bob));
        List<Callable<Object>> commands = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            List<Long> order = orders.get(i % orders.size());
            commands.add(() -> meetingService.book(order.getFirst(), "Sync", null, order.subList(1, 3), slot));
        }

        List<Object> outcomes = runConcurrently(commands);

        assertThat(outcomes).filteredOn(outcome -> !(outcome instanceof Throwable)).hasSize(1);
        assertThat(outcomes).filteredOn(Throwable.class::isInstance)
                .hasSize(THREADS - 1)
                .allMatch(ParticipantsNotFreeException.class::isInstance);
        List<EventEntity> meetings = meetingEvents(List.of(alice, bob, carol), slot);
        assertThat(meetings).hasSize(3);
        assertThat(meetings).extracting(EventEntity::meetingId).containsOnly(meetings.getFirst().meetingId());
    }

    @Test
    void parallelMarkBusyAndMeetingForSameUserExactlyOneSucceeds() throws Exception {
        long alice = newUser();
        long bob = newUser();

        // a pair of commands races on every slot, to make the race likely without the lock
        for (int hour = 0; hour < 20; hour++) {
            TimeRange slot = range(hour, hour + 1);
            markFree(List.of(alice, bob), slot);

            List<Object> outcomes = runConcurrently(List.of(
                    () -> eventService.mark(alice, EventType.MARKED_BUSY, slot),
                    () -> meetingService.book(bob, "Sync", null, List.of(alice), slot)));

            assertThat(outcomes).as("slot %s", slot)
                    .filteredOn(outcome -> !(outcome instanceof Throwable)).hasSize(1);
            assertThat(outcomes).as("slot %s", slot)
                    .filteredOn(Throwable.class::isInstance).singleElement()
                    .isInstanceOfAny(TimeRangeHasMeetingsException.class, ParticipantsNotFreeException.class);
        }
    }

    /**
     * Starts all commands at once and waits for them.
     *
     * @return per command: its result, or the exception it has thrown
     */
    private static List<Object> runConcurrently(List<Callable<Object>> commands) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(commands.size())) {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> command : commands) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        return command.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            start.countDown();

            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        }
    }

    private long newUser() {
        return userRepository.save(new User(null, "user-" + UUID.randomUUID())).id();
    }

    private void markFree(List<Long> userIds, TimeRange timeRange) {
        userIds.forEach(userId -> eventService.mark(userId, EventType.MARKED_FREE, timeRange));
    }

    private List<EventEntity> meetingEvents(List<Long> userIds, TimeRange timeRange) {
        return eventRepository.findOverlapping(userIds, timeRange.startTime(), timeRange.endTime()).stream()
                .filter(event -> event.eventType() == EventType.MEETING_BOOKED)
                .toList();
    }

    private static TimeRange range(int startHour, int endHour) {
        LocalDateTime day = TOMORROW.atStartOfDay();
        return new TimeRange(day.plusHours(startHour), day.plusHours(endHour));
    }
}
