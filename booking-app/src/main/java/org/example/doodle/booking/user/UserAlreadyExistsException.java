package org.example.doodle.booking.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UserAlreadyExistsException extends ResponseStatusException {

    public UserAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "User with name '" + name + "' already exists");
    }
}
