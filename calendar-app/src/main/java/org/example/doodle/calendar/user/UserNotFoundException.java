package org.example.doodle.calendar.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserNotFoundException extends ResponseStatusException {

    public UserNotFoundException(long userId) {
        super(HttpStatus.NOT_FOUND, "User " + userId + " not found");
    }
}
