package org.example.doodle.calendar.meeting;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MeetingNotFoundException extends ResponseStatusException {

    public MeetingNotFoundException(long meetingId) {
        super(HttpStatus.NOT_FOUND, "Meeting " + meetingId + " not found");
    }
}
