package org.example.doodle.booking.event;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

public class StartTimeInPastException extends ResponseStatusException {

    public StartTimeInPastException(LocalDateTime startTime) {
        super(HttpStatus.BAD_REQUEST, "startTime must be in the future: " + startTime);
    }
}
