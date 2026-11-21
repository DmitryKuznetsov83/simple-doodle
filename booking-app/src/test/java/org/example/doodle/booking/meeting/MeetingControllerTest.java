package org.example.doodle.booking.meeting;

import org.example.doodle.booking.ApiExceptionHandler;
import org.example.doodle.booking.ProjectionConfig;
import org.example.doodle.booking.event.EventEntity;
import org.example.doodle.booking.event.EventRepository;
import org.example.doodle.booking.event.ProjectionLoader;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
@Import({MeetingService.class, ProjectionLoader.class, ProjectionConfig.class, ApiExceptionHandler.class,
        MeetingControllerTest.FixedClock.class})
class MeetingControllerTest {

    /** "Now" is 2026-11-21 12:00. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final long ORGANIZER_ID = 1;
    private static final long MEETING_ID = 42;
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 22, 10, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 22, 11, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 21, 12, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MeetingRepository meetingRepository;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @SuppressWarnings("unchecked")
    void booksMeetingForAllParticipantsIncludingOrganizer() throws Exception {
        givenAllExist();
        givenAllFree();
        when(meetingRepository.save(any())).thenReturn(new MeetingEntity(MEETING_ID, "Sync", "Weekly"));
        when(eventRepository.saveAll(anyIterable())).then(returnsFirstArg());

        // duplicates and the organizer in the list are collapsed
        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[2, 3, 2, 1]", "2026-11-22T10:00:00", "2026-11-22T11:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").value(MEETING_ID))
                .andExpect(jsonPath("$.organizerId").value(ORGANIZER_ID))
                .andExpect(jsonPath("$.participantIds").value(org.hamcrest.Matchers.contains(1, 2, 3)));

        verify(meetingRepository).save(new MeetingEntity(null, "Sync", "Weekly"));
        var saved = forClass(Iterable.class);
        verify(eventRepository).saveAll(saved.capture());
        assertThat((Iterable<EventEntity>) saved.getValue()).containsExactly(
                meetingBooked(1), meetingBooked(2), meetingBooked(3));
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyParticipantsBooksMeetingForOrganizerOnly() throws Exception {
        givenAllExist();
        givenAllFree();
        when(meetingRepository.save(any())).thenReturn(new MeetingEntity(MEETING_ID, "Sync", "Weekly"));
        when(eventRepository.saveAll(anyIterable())).then(returnsFirstArg());

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[]", "2026-11-22T10:00:00", "2026-11-22T11:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.participantIds").value(org.hamcrest.Matchers.contains(1)));

        var saved = forClass(Iterable.class);
        verify(eventRepository).saveAll(saved.capture());
        assertThat((Iterable<EventEntity>) saved.getValue()).containsExactly(meetingBooked(1));
    }

    @Test
    void rejectsParticipantsNotEntirelyFree() throws Exception {
        givenAllExist();
        when(eventRepository.findOverlapping(anyCollection(), any(), any())).thenReturn(List.of(
                // organizer (1): no events at all, i.e. busy
                // 2: free, but part of the range is busy
                event(2, EventType.MARKED_FREE, null, 9, 0, 12, 0),
                event(2, EventType.MARKED_BUSY, null, 10, 45, 11, 0),
                // 3: free except for another meeting
                event(3, EventType.MARKED_FREE, null, 9, 0, 12, 0),
                event(3, EventType.MEETING_BOOKED, 7L, 10, 0, 10, 15),
                // 4: free
                event(4, EventType.MARKED_FREE, null, 10, 0, 11, 0)));

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[2, 3, 4]", "2026-11-22T10:00:00", "2026-11-22T11:00:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("[1, 2, 3]")));

        verifyNothingSaved();
    }

    @Test
    void rejectsUnknownParticipants() throws Exception {
        when(userRepository.lockAllById(anyCollection())).thenReturn(List.of(1L, 2L));

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[2, 3, 4]", "2026-11-22T10:00:00", "2026-11-22T11:00:00")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("[3, 4]")));

        verifyNothingSaved();
    }

    @Test
    void rejectsUnknownOrganizer() throws Exception {
        // the participant exists, the organizer does not
        when(userRepository.lockAllById(anyCollection())).thenReturn(List.of(2L));

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[2]", "2026-11-22T10:00:00", "2026-11-22T11:00:00")))
                .andExpect(status().isNotFound());

        verifyNothingSaved();
    }

    @Test
    void rejectsStartTimeNotInFuture() throws Exception {
        givenAllExist();

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[2]", "2026-11-21T12:00:00", "2026-11-21T13:00:00")))
                .andExpect(status().isBadRequest());

        verifyNothingSaved();
    }

    @Test
    void rejectsRangeNotAlignedToQuantum() throws Exception {
        givenAllExist();

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Sync", "[2]", "2026-11-22T10:10:00", "2026-11-22T11:00:00")))
                .andExpect(status().isBadRequest());

        verifyNothingSaved();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // blank title
            """
                    {"title": " ", "participantIds": [2], "startTime": "2026-11-22T10:00:00", "endTime": "2026-11-22T11:00:00"}""",
            // missing participantIds
            """
                    {"title": "Sync", "startTime": "2026-11-22T10:00:00", "endTime": "2026-11-22T11:00:00"}""",
            // null participant id
            """
                    {"title": "Sync", "participantIds": [null], "startTime": "2026-11-22T10:00:00", "endTime": "2026-11-22T11:00:00"}""",
            // missing endTime
            """
                    {"title": "Sync", "participantIds": [2], "startTime": "2026-11-22T10:00:00"}"""
    })
    void rejectsInvalidRequest(String body) throws Exception {
        givenAllExist();

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNothingSaved();
    }

    @Test
    void rejectsTooLongTitle() throws Exception {
        givenAllExist();

        mvc.perform(post("/booking/users/{userId}/meetings", ORGANIZER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("x".repeat(256), "[2]", "2026-11-22T10:00:00", "2026-11-22T11:00:00")))
                .andExpect(status().isBadRequest());

        verifyNothingSaved();
    }

    private void verifyNothingSaved() {
        verify(meetingRepository, never()).save(any());
        verify(eventRepository, never()).saveAll(anyIterable());
    }

    /** Every requested user exists. */
    private void givenAllExist() {
        when(userRepository.lockAllById(anyCollection())).thenAnswer(inv -> {
            Collection<Long> userIds = inv.getArgument(0);
            return userIds.stream().sorted().toList();
        });
    }

    /** Every requested user has marked 09:00-12:00 as free. */
    private void givenAllFree() {
        when(eventRepository.findOverlapping(anyCollection(), any(), any())).thenAnswer(inv -> {
            Collection<Long> userIds = inv.getArgument(0);
            return userIds.stream()
                    .map(userId -> event(userId, EventType.MARKED_FREE, null, 9, 0, 12, 0))
                    .toList();
        });
    }

    private static EventEntity event(long userId, EventType eventType, Long meetingId,
                                     int startHour, int startMinute, int endHour, int endMinute) {
        return new EventEntity(null, userId, userId, eventType, meetingId,
                LocalDateTime.of(2026, 11, 22, startHour, startMinute),
                LocalDateTime.of(2026, 11, 22, endHour, endMinute),
                LocalDateTime.of(2026, 11, 20, 12, 0));
    }

    private static EventEntity meetingBooked(long userId) {
        return new EventEntity(null, ORGANIZER_ID, userId, EventType.MEETING_BOOKED, MEETING_ID, START, END, NOW);
    }

    private static String body(String title, String participantIds, String startTime, String endTime) {
        return """
                {"title": "%s", "description": "Weekly", "participantIds": %s, "startTime": "%s", "endTime": "%s"}"""
                .formatted(title, participantIds, startTime, endTime);
    }
}
