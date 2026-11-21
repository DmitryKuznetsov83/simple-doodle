package org.example.doodle.core.projection;

/**
 * State of a time interval in a user's calendar.
 */
public enum TimeState {
    /** Available. */
    FREE,
    /** Not available. Time not covered by any event is busy as well. */
    BUSY,
    /** Occupied by a meeting. */
    MEETING
}
