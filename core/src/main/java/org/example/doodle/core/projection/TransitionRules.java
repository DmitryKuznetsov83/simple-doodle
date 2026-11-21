package org.example.doodle.core.projection;

import org.example.doodle.core.event.EventType;

/**
 * State transition rules: whether an event may be appended to a user's event log.
 * <p>
 * The projection must be built with the window equal to the event's time range.
 * <ul>
 *     <li>{@link EventType#MARKED_FREE}, {@link EventType#MARKED_BUSY} - the window has no meetings</li>
 *     <li>{@link EventType#MEETING_BOOKED} - the whole window is {@link TimeState#FREE}</li>
 * </ul>
 */
public final class TransitionRules {

    private TransitionRules() {
    }

    public static boolean allows(EventType eventType, Projection projection) {
        return switch (eventType) {
            case MARKED_FREE, MARKED_BUSY -> projection.intervals().stream()
                    .noneMatch(interval -> interval.state() == TimeState.MEETING);
            case MEETING_BOOKED -> projection.intervals().size() == 1
                    && projection.intervals().getFirst().state() == TimeState.FREE;
        };
    }
}
