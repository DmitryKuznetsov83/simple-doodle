package org.example.doodle.calendar.meeting;

import org.example.doodle.calendar.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeetingController.class)
@Import({MeetingService.class, ApiExceptionHandler.class})
class MeetingControllerTest {

    private static final long MEETING_ID = 42;
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 22, 11, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MeetingRepository repository;

    @Test
    void returnsMeetingWithOrganizerAndParticipants() throws Exception {
        when(repository.findMeeting(MEETING_ID)).thenReturn(List.of(
                row("Weekly", 2, "Paul"),
                row("Weekly", 3, "George"),
                row("Weekly", 1, "John")));

        mvc.perform(get("/calendar/meetings/{meetingId}", MEETING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").value(MEETING_ID))
                .andExpect(jsonPath("$.title").value("Sync"))
                .andExpect(jsonPath("$.description").value("Weekly"))
                .andExpect(jsonPath("$.organizer.id").value(2))
                .andExpect(jsonPath("$.organizer.name").value("Paul"))
                .andExpect(jsonPath("$.startTime").value("2026-11-22T11:00:00"))
                .andExpect(jsonPath("$.endTime").value("2026-11-22T12:00:00"))
                .andExpect(jsonPath("$.participants.length()").value(3))
                .andExpect(jsonPath("$.participants[0].id").value(2))
                .andExpect(jsonPath("$.participants[0].name").value("Paul"))
                .andExpect(jsonPath("$.participants[1].id").value(3))
                .andExpect(jsonPath("$.participants[1].name").value("George"))
                .andExpect(jsonPath("$.participants[2].id").value(1))
                .andExpect(jsonPath("$.participants[2].name").value("John"));
    }

    @Test
    void omitsMissingDescription() throws Exception {
        when(repository.findMeeting(MEETING_ID)).thenReturn(List.of(row(null, 2, "Paul")));

        mvc.perform(get("/calendar/meetings/{meetingId}", MEETING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    void rejectsUnknownMeeting() throws Exception {
        when(repository.findMeeting(MEETING_ID)).thenReturn(List.of());

        mvc.perform(get("/calendar/meetings/{meetingId}", MEETING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Meeting 42 not found"));
    }

    @Test
    void rejectsNonNumericMeetingId() throws Exception {
        mvc.perform(get("/calendar/meetings/{meetingId}", "sync"))
                .andExpect(status().isBadRequest());

        verify(repository, never()).findMeeting(anyLong());
    }

    private static MeetingRow row(String description, long participantId, String participantName) {
        return new MeetingRow(MEETING_ID, "Sync", description, 2, "Paul", START, END,
                participantId, participantName);
    }
}
