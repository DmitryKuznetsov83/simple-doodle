package org.example.doodle.calendar.user;

import org.example.doodle.calendar.CalendarProperties;
import org.example.doodle.core.projection.Projection;
import org.example.doodle.core.projection.ProjectionBuilder;
import org.example.doodle.core.time.TimeRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserCalendarService {

    private final UserCalendarRepository repository;
    private final ProjectionBuilder projectionBuilder;
    private final Duration maxWindow;

    public UserCalendarService(UserCalendarRepository repository, ProjectionBuilder projectionBuilder,
                               CalendarProperties properties) {
        this.repository = repository;
        this.projectionBuilder = projectionBuilder;
        this.maxWindow = properties.maxWindow();
    }

    /**
     * The user's calendar within the window: projection of the user's event log.
     */
    @Transactional(readOnly = true)
    public UserCalendarResponse calendar(long userId, TimeRange window) {
        if (Duration.between(window.startTime(), window.endTime()).compareTo(maxWindow) > 0) {
            throw new WindowTooLongException(maxWindow);
        }
        User user = repository.findUser(userId).orElseThrow(() -> new UserNotFoundException(userId));

        List<EventRow> events = repository.findEvents(userId, window);
        Projection projection = projectionBuilder.build(window, events.stream().map(EventRow::toEvent).toList());

        Map<Long, String> meetingTitles = new HashMap<>();
        events.stream()
                .filter(event -> event.meetingId() != null)
                .forEach(event -> meetingTitles.put(event.meetingId(), event.meetingTitle()));

        List<UserCalendarResponse.Interval> intervals = projection.intervals().stream()
                .map(interval -> new UserCalendarResponse.Interval(
                        interval.timeRange().startTime(),
                        interval.timeRange().endTime(),
                        interval.state(),
                        interval.meetingId(),
                        meetingTitles.get(interval.meetingId())))
                .toList();

        return new UserCalendarResponse(user, window.startTime(), window.endTime(), intervals);
    }
}
