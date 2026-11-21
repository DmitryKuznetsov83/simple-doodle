package org.example.doodle.booking.event;

import org.example.doodle.booking.ApiExceptionHandler;
import org.example.doodle.booking.ProjectionConfig;
import org.example.doodle.booking.user.UserRepository;
import org.example.doodle.core.event.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({EventService.class, ProjectionLoader.class, ProjectionConfig.class, ApiExceptionHandler.class,
        EventControllerTest.FixedClock.class})
class EventControllerTest {

    /** "Now" is 2026-11-21 12:00. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final long USER_ID = 1;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private UserRepository userRepository;

    @ParameterizedTest
    @CsvSource({"free, MARKED_FREE", "busy, MARKED_BUSY"})
    void appendsEventToOwnCalendar(String path, EventType expectedType) throws Exception {
        when(userRepository.lockAllById(List.of(USER_ID))).thenReturn(List.of(USER_ID));
        when(eventRepository.save(any())).then(returnsFirstArg());

        mvc.perform(post("/booking/users/{userId}/" + path, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:00:00", "2026-11-22T10:30:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value(expectedType.name()));

        var saved = forClass(EventEntity.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue()).isEqualTo(new EventEntity(
                null, USER_ID, USER_ID, expectedType, null,
                LocalDateTime.of(2026, 11, 22, 10, 0), LocalDateTime.of(2026, 11, 22, 10, 30),
                LocalDateTime.of(2026, 11, 21, 12, 0)));
    }

    @ParameterizedTest
    @CsvSource({"free", "busy"})
    void rejectsRangeOverlappingMeeting(String path) throws Exception {
        when(userRepository.lockAllById(List.of(USER_ID))).thenReturn(List.of(USER_ID));
        when(eventRepository.findOverlapping(anyCollection(), any(), any())).thenReturn(List.of(
                event(EventType.MARKED_FREE, null, 9, 0, 12, 0),
                event(EventType.MEETING_BOOKED, 7L, 10, 15, 11, 0)));

        // partial overlap rejects the whole request
        mvc.perform(post("/booking/users/{userId}/" + path, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:00:00", "2026-11-22T10:30:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("meetings [7]")));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void allowsRangeAdjacentToMeeting() throws Exception {
        when(userRepository.lockAllById(List.of(USER_ID))).thenReturn(List.of(USER_ID));
        when(eventRepository.findOverlapping(anyCollection(), any(), any())).thenReturn(List.of(
                event(EventType.MEETING_BOOKED, 7L, 10, 30, 11, 0)));
        when(eventRepository.save(any())).then(returnsFirstArg());

        mvc.perform(post("/booking/users/{userId}/busy", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:00:00", "2026-11-22T10:30:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsRangeNotAlignedToQuantum() throws Exception {
        when(userRepository.lockAllById(List.of(USER_ID))).thenReturn(List.of(USER_ID));

        mvc.perform(post("/booking/users/{userId}/free", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:10:00", "2026-11-22T10:30:00")))
                .andExpect(status().isBadRequest());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void rejectsStartTimeNotInFuture() throws Exception {
        when(userRepository.lockAllById(List.of(USER_ID))).thenReturn(List.of(USER_ID));

        mvc.perform(post("/booking/users/{userId}/free", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-21T12:00:00", "2026-11-21T13:00:00")))
                .andExpect(status().isBadRequest());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownUser() throws Exception {
        when(userRepository.lockAllById(List.of(USER_ID))).thenReturn(List.of());

        mvc.perform(post("/booking/users/{userId}/free", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:00:00", "2026-11-22T10:30:00")))
                .andExpect(status().isNotFound());

        verify(eventRepository, never()).save(any());
    }

    private static EventEntity event(EventType eventType, Long meetingId,
                                     int startHour, int startMinute, int endHour, int endMinute) {
        return new EventEntity(null, USER_ID, USER_ID, eventType, meetingId,
                LocalDateTime.of(2026, 11, 22, startHour, startMinute),
                LocalDateTime.of(2026, 11, 22, endHour, endMinute),
                LocalDateTime.of(2026, 11, 20, 12, 0));
    }

    private static String body(String startTime, String endTime) {
        return """
                {"startTime": "%s", "endTime": "%s"}""".formatted(startTime, endTime);
    }
}
