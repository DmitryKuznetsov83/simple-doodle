package org.example.doodle.booking.meeting;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

/**
 * Participant ids come from the request body, not from the path, so it is 422 rather than 404.
 */
public class ParticipantsNotFoundException extends ResponseStatusException {

    public ParticipantsNotFoundException(Collection<Long> participantIds) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Participants not found: " + participantIds);
    }
}
