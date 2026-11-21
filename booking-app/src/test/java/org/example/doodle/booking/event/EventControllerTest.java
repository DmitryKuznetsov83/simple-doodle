package org.example.doodle.booking.event;

import org.example.doodle.booking.ApiExceptionHandler;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({EventService.class, ApiExceptionHandler.class, EventControllerTest.FixedClock.class})
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
        when(userRepository.existsById(USER_ID)).thenReturn(true);
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

    @Test
    void rejectsRangeNotAlignedToQuantum() throws Exception {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        mvc.perform(post("/booking/users/{userId}/free", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:10:00", "2026-11-22T10:30:00")))
                .andExpect(status().isBadRequest());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void rejectsStartTimeNotInFuture() throws Exception {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        mvc.perform(post("/booking/users/{userId}/free", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-21T12:00:00", "2026-11-21T13:00:00")))
                .andExpect(status().isBadRequest());

        verify(eventRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownUser() throws Exception {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        mvc.perform(post("/booking/users/{userId}/free", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-22T10:00:00", "2026-11-22T10:30:00")))
                .andExpect(status().isNotFound());

        verify(eventRepository, never()).save(any());
    }

    private static String body(String startTime, String endTime) {
        return """
                {"startTime": "%s", "endTime": "%s"}""".formatted(startTime, endTime);
    }
}
