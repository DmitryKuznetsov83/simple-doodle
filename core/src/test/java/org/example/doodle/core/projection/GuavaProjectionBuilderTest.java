package org.example.doodle.core.projection;

import org.example.doodle.core.DomainValidationException;
import org.example.doodle.core.event.Event;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuavaProjectionBuilderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 21);
    private static final TimeRange WINDOW = range("09:00", "13:00");

    private final ProjectionBuilder builder = new GuavaProjectionBuilder();

    @Test
    void uncoveredTimeIsBusy() {
        Projection projection = builder.build(WINDOW, List.of());

        assertThat(projection.window()).isEqualTo(WINDOW);
        assertThat(projection.intervals()).containsExactly(busy("09:00", "13:00"));
    }

    @Test
    void buildsProjectionFromSpecExample() {
        assertThat(build(
                markedFree("09:00", "12:00"),
                markedBusy("10:00", "10:30"),
                meetingBooked("11:00", "12:00", 1L))).containsExactly(
                free("09:00", "10:00"),
                busy("10:00", "10:30"),
                free("10:30", "11:00"),
                meeting("11:00", "12:00", 1L),
                busy("12:00", "13:00"));
    }

    @Test
    void ignoresEventsOutsideWindow() {
        assertThat(build(
                markedFree("07:00", "08:00"),
                markedFree("08:00", "09:00"),
                markedFree("13:00", "14:00"),
                markedFree("14:00", "15:00"))).containsExactly(
                busy("09:00", "13:00"));
    }

    @Test
    void clipsEventsToWindow() {
        assertThat(build(
                markedFree("08:00", "10:00"),
                meetingBooked("12:00", "14:00", 1L))).containsExactly(
                free("09:00", "10:00"),
                busy("10:00", "12:00"),
                meeting("12:00", "13:00", 1L));
    }

    @Test
    void mergesAdjacentEventsOfSameState() {
        assertThat(build(
                markedFree("10:00", "11:00"),
                markedFree("11:00", "12:00"))).containsExactly(
                busy("09:00", "10:00"),
                free("10:00", "12:00"),
                busy("12:00", "13:00"));
    }

    @Test
    void doesNotMergeAdjacentDifferentMeetings() {
        assertThat(build(
                meetingBooked("09:00", "11:00", 1L),
                meetingBooked("11:00", "13:00", 2L))).containsExactly(
                meeting("09:00", "11:00", 1L),
                meeting("11:00", "13:00", 2L));
    }

    @Test
    void rejectsNullArguments() {
        assertThatThrownBy(() -> builder.build(null, List.of()))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> builder.build(WINDOW, null))
                .isInstanceOf(DomainValidationException.class);
    }

    private List<ProjectionInterval> build(Event... events) {
        return builder.build(WINDOW, List.of(events)).intervals();
    }

    private static Event markedFree(String start, String end) {
        return new Event(EventType.MARKED_FREE, range(start, end), null);
    }

    private static Event markedBusy(String start, String end) {
        return new Event(EventType.MARKED_BUSY, range(start, end), null);
    }

    private static Event meetingBooked(String start, String end, long meetingId) {
        return new Event(EventType.MEETING_BOOKED, range(start, end), meetingId);
    }

    private static ProjectionInterval free(String start, String end) {
        return new ProjectionInterval(range(start, end), TimeState.FREE, null);
    }

    private static ProjectionInterval busy(String start, String end) {
        return new ProjectionInterval(range(start, end), TimeState.BUSY, null);
    }

    private static ProjectionInterval meeting(String start, String end, long meetingId) {
        return new ProjectionInterval(range(start, end), TimeState.MEETING, meetingId);
    }

    private static TimeRange range(String start, String end) {
        return new TimeRange(time(start), time(end));
    }

    private static LocalDateTime time(String time) {
        return LocalDateTime.of(DAY, LocalTime.parse(time));
    }
}
