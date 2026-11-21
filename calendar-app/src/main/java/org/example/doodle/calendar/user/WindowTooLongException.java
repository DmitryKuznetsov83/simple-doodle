package org.example.doodle.calendar.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

public class WindowTooLongException extends ResponseStatusException {

    public WindowTooLongException(Duration maxWindow) {
        super(HttpStatus.BAD_REQUEST, "Window must not exceed " + maxWindow.toDays() + " days");
    }
}
