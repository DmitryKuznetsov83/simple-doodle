package org.example.doodle.booking.event;

import org.example.doodle.booking.user.UserNotFoundException;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public EventService(EventRepository eventRepository, UserRepository userRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * Appends a {@link EventType#MARKED_FREE} or {@link EventType#MARKED_BUSY} event
     * to the user's own calendar.
     */
    @Transactional
    public EventEntity mark(long userId, EventType eventType, TimeRange timeRange) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!timeRange.startTime().isAfter(now)) {
            throw new StartTimeInPastException(timeRange.startTime());
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return eventRepository.save(new EventEntity(
                null, userId, userId, eventType, null,
                timeRange.startTime(), timeRange.endTime(), now));
    }
}
