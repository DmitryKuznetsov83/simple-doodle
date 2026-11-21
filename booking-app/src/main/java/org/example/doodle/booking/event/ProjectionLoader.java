package org.example.doodle.booking.event;

import org.example.doodle.core.event.Event;
import org.example.doodle.core.projection.Projection;
import org.example.doodle.core.projection.ProjectionBuilder;
import org.example.doodle.core.time.TimeRange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds users' projections within a window from the event log, with one query for all users.
 */
@Component
public class ProjectionLoader {

    private final EventRepository eventRepository;
    private final ProjectionBuilder projectionBuilder;

    public ProjectionLoader(EventRepository eventRepository, ProjectionBuilder projectionBuilder) {
        this.eventRepository = eventRepository;
        this.projectionBuilder = projectionBuilder;
    }

    /**
     * @return projection per user, in the order of {@code userIds}
     */
    public Map<Long, Projection> load(Collection<Long> userIds, TimeRange window) {
        Map<Long, List<Event>> eventsByUser = new LinkedHashMap<>();
        userIds.forEach(userId -> eventsByUser.put(userId, new ArrayList<>()));
        eventRepository.findOverlapping(userIds, window.startTime(), window.endTime())
                .forEach(event -> eventsByUser.get(event.userId()).add(event.toEvent()));

        Map<Long, Projection> projections = new LinkedHashMap<>();
        eventsByUser.forEach((userId, events) -> projections.put(userId, projectionBuilder.build(window, events)));
        return projections;
    }

    public Projection load(long userId, TimeRange window) {
        return load(List.of(userId), window).get(userId);
    }
}
