package org.example.doodle.booking.event;

import org.example.doodle.booking.user.UserNotFoundException;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.projection.Projection;
import org.example.doodle.core.projection.ProjectionInterval;
import org.example.doodle.core.projection.TransitionRules;
import org.example.doodle.core.time.TimeRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProjectionLoader projectionLoader;
    private final Clock clock;

    public EventService(EventRepository eventRepository, UserRepository userRepository,
                        ProjectionLoader projectionLoader, Clock clock) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.projectionLoader = projectionLoader;
        this.clock = clock;
    }

    /**
     * Appends a {@link EventType#MARKED_FREE} or {@link EventType#MARKED_BUSY} event
     * to the user's own calendar.
     *
     * @throws TimeRangeHasMeetingsException if the time range overlaps a meeting
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public EventEntity mark(long userId, EventType eventType, TimeRange timeRange) {
        LocalDateTime now = LocalDateTime.now(clock);
        BookingRules.requireStartInFuture(timeRange, now);
        // the user's commands are serialized; events are read after the lock, so they are up to date
        if (userRepository.lockAllById(List.of(userId)).isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        Projection projection = projectionLoader.load(userId, timeRange);
        if (!TransitionRules.allows(eventType, projection)) {
            throw new TimeRangeHasMeetingsException(timeRange, meetingIds(projection));
        }

        return eventRepository.save(new EventEntity(
                null, userId, userId, eventType, null,
                timeRange.startTime(), timeRange.endTime(), now));
    }

    private static List<Long> meetingIds(Projection projection) {
        return projection.intervals().stream()
                .map(ProjectionInterval::meetingId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
