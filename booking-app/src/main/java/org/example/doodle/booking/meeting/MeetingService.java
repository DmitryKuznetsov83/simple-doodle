package org.example.doodle.booking.meeting;

import org.example.doodle.booking.event.BookingRules;
import org.example.doodle.booking.event.EventEntity;
import org.example.doodle.booking.event.EventRepository;
import org.example.doodle.booking.event.ProjectionLoader;
import org.example.doodle.booking.user.UserNotFoundException;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.projection.Projection;
import org.example.doodle.core.projection.TransitionRules;
import org.example.doodle.core.time.TimeRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProjectionLoader projectionLoader;
    private final Clock clock;

    public MeetingService(MeetingRepository meetingRepository, EventRepository eventRepository,
                          UserRepository userRepository, ProjectionLoader projectionLoader, Clock clock) {
        this.meetingRepository = meetingRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.projectionLoader = projectionLoader;
        this.clock = clock;
    }

    /**
     * Creates a meeting and appends one {@link EventType#MEETING_BOOKED} event per participant,
     * the organizer included.
     *
     * @throws ParticipantsNotFreeException if the time range is not entirely free for some participants
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MeetingResponse book(long organizerId, String title, String description,
                                List<Long> participantIds, TimeRange timeRange) {
        LocalDateTime now = LocalDateTime.now(clock);
        BookingRules.requireStartInFuture(timeRange, now);

        Set<Long> participants = new LinkedHashSet<>();
        participants.add(organizerId);
        participants.addAll(participantIds);
        lockAllExisting(organizerId, participants);
        requireAllFree(participants, timeRange);

        MeetingEntity meeting = meetingRepository.save(new MeetingEntity(null, title, description));
        List<EventEntity> events = participants.stream()
                .map(participantId -> new EventEntity(
                        null, organizerId, participantId, EventType.MEETING_BOOKED, meeting.id(),
                        timeRange.startTime(), timeRange.endTime(), now))
                .toList();
        eventRepository.saveAll(events);

        return new MeetingResponse(meeting.id(), organizerId, title, description,
                List.copyOf(participants), timeRange.startTime(), timeRange.endTime());
    }

    private void requireAllFree(Set<Long> participants, TimeRange timeRange) {
        Map<Long, Projection> projections = projectionLoader.load(participants, timeRange);
        List<Long> notFree = projections.entrySet().stream()
                .filter(entry -> !TransitionRules.allows(EventType.MEETING_BOOKED, entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        if (!notFree.isEmpty()) {
            throw new ParticipantsNotFreeException(notFree);
        }
    }

    /**
     * Locks all participants, so their commands are serialized; events are read after the lock,
     * so they are up to date.
     */
    private void lockAllExisting(long organizerId, Set<Long> participants) {
        Set<Long> locked = new HashSet<>(userRepository.lockAllById(participants));
        if (!locked.contains(organizerId)) {
            throw new UserNotFoundException(organizerId);
        }
        List<Long> missing = new ArrayList<>(participants);
        missing.removeAll(locked);
        if (!missing.isEmpty()) {
            throw new ParticipantsNotFoundException(missing);
        }
    }
}
