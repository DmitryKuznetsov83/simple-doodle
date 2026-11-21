package org.example.doodle.calendar.user;

import org.example.doodle.calendar.ApiExceptionHandler;
import org.example.doodle.calendar.CalendarConfig;
import org.example.doodle.core.event.EventType;
import org.example.doodle.core.time.TimeRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCalendarController.class)
@Import({UserCalendarService.class, CalendarConfig.class, ApiExceptionHandler.class})
class UserCalendarControllerTest {

    private static final long USER_ID = 1;
    private static final User USER = new User(USER_ID, "John");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserCalendarRepository repository;

    @Test
    void returnsProjectionOfEventsWithinWindow() throws Exception {
        TimeRange window = new TimeRange(at(9, 0), at(13, 0));
        when(repository.findUser(USER_ID)).thenReturn(Optional.of(USER));
        when(repository.findEvents(USER_ID, window)).thenReturn(List.of(
                new EventRow(EventType.MARKED_FREE, at(9, 0), at(12, 0), null, null),
                new EventRow(EventType.MARKED_BUSY, at(10, 0), at(10, 30), null, null),
                new EventRow(EventType.MEETING_BOOKED, at(11, 0), at(12, 0), 42L, "Sync")));

        mvc.perform(get("/calendar/users/{userId}", USER_ID)
                        .param("startTime", "2026-11-22T09:00:00")
                        .param("endTime", "2026-11-22T13:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(USER_ID))
                .andExpect(jsonPath("$.user.name").value("John"))
                .andExpect(jsonPath("$.startTime").value("2026-11-22T09:00:00"))
                .andExpect(jsonPath("$.endTime").value("2026-11-22T13:00:00"))
                .andExpect(jsonPath("$.intervals.length()").value(5))
                .andExpect(jsonPath("$.intervals[0].startTime").value("2026-11-22T09:00:00"))
                .andExpect(jsonPath("$.intervals[0].endTime").value("2026-11-22T10:00:00"))
                .andExpect(jsonPath("$.intervals[0].state").value("FREE"))
                .andExpect(jsonPath("$.intervals[0].meetingId").doesNotExist())
                .andExpect(jsonPath("$.intervals[0].meetingTitle").doesNotExist())
                .andExpect(jsonPath("$.intervals[1].state").value("BUSY"))
                .andExpect(jsonPath("$.intervals[2].state").value("FREE"))
                .andExpect(jsonPath("$.intervals[3].startTime").value("2026-11-22T11:00:00"))
                .andExpect(jsonPath("$.intervals[3].endTime").value("2026-11-22T12:00:00"))
                .andExpect(jsonPath("$.intervals[3].state").value("MEETING"))
                .andExpect(jsonPath("$.intervals[3].meetingId").value(42))
                .andExpect(jsonPath("$.intervals[3].meetingTitle").value("Sync"))
                .andExpect(jsonPath("$.intervals[4].state").value("BUSY"))
                .andExpect(jsonPath("$.intervals[4].endTime").value("2026-11-22T13:00:00"));
    }

    @Test
    void windowWithoutEventsIsBusy() throws Exception {
        when(repository.findUser(USER_ID)).thenReturn(Optional.of(USER));
        when(repository.findEvents(anyLong(), any())).thenReturn(List.of());

        mvc.perform(get("/calendar/users/{userId}", USER_ID)
                        .param("startTime", "2026-11-22T09:00:00")
                        .param("endTime", "2026-11-22T13:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervals.length()").value(1))
                .andExpect(jsonPath("$.intervals[0].state").value("BUSY"));
    }

    @Test
    void windowOfMaxLengthIsAllowed() throws Exception {
        when(repository.findUser(USER_ID)).thenReturn(Optional.of(USER));
        when(repository.findEvents(anyLong(), any())).thenReturn(List.of());

        mvc.perform(get("/calendar/users/{userId}", USER_ID)
                        .param("startTime", "2026-11-01T00:00:00")
                        .param("endTime", "2026-12-02T00:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsWindowLongerThanMax() throws Exception {
        mvc.perform(get("/calendar/users/{userId}", USER_ID)
                        .param("startTime", "2026-11-01T00:00:00")
                        .param("endTime", "2026-12-02T00:15:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Window must not exceed 31 days"));

        verify(repository, never()).findEvents(anyLong(), any());
    }

    @Test
    void rejectsUnknownUser() throws Exception {
        when(repository.findUser(USER_ID)).thenReturn(Optional.empty());

        mvc.perform(get("/calendar/users/{userId}", USER_ID)
                        .param("startTime", "2026-11-22T09:00:00")
                        .param("endTime", "2026-11-22T13:00:00"))
                .andExpect(status().isNotFound());

        verify(repository, never()).findEvents(anyLong(), any());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            // missing params
            "null, 2026-11-22T13:00:00",
            "2026-11-22T09:00:00, null",
            // not a date-time
            "tomorrow, 2026-11-22T13:00:00",
            // not aligned to the quantum
            "2026-11-22T09:10:00, 2026-11-22T13:00:00",
            // end is not after start
            "2026-11-22T13:00:00, 2026-11-22T13:00:00",
            "2026-11-22T13:00:00, 2026-11-22T09:00:00"
    })
    void rejectsInvalidWindow(String startTime, String endTime) throws Exception {
        var request = get("/calendar/users/{userId}", USER_ID);
        if (startTime != null) {
            request.param("startTime", startTime);
        }
        if (endTime != null) {
            request.param("endTime", endTime);
        }

        mvc.perform(request).andExpect(status().isBadRequest());

        verify(repository, never()).findEvents(anyLong(), any());
    }

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 22, hour, minute);
    }
}
