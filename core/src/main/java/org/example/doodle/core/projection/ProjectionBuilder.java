package org.example.doodle.core.projection;

import org.example.doodle.core.event.Event;
import org.example.doodle.core.time.TimeRange;

import java.util.List;

/**
 * Builds a user's {@link Projection} within a window from the event log.
 * <p>
 * Contract:
 * <ul>
 *     <li>events are applied in list order, a later event overrides earlier ones</li>
 *     <li>time not covered by any event is {@link TimeState#BUSY}</li>
 *     <li>events are clipped to the window, events entirely outside it are ignored</li>
 *     <li>adjacent intervals with the same state are merged; different meetings are never merged</li>
 * </ul>
 * Preparing events is the caller's responsibility: events of a single user, in log order (by id).
 * Events are applied as is, booking rules are not checked here.
 */
public interface ProjectionBuilder {

    Projection build(TimeRange window, List<Event> events);
}
