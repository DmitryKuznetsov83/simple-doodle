package org.example.doodle.booking.meeting;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

/**
 * A meeting can be booked only on time that is entirely free for every participant.
 */
public class ParticipantsNotFreeException extends ResponseStatusException {

    public ParticipantsNotFreeException(Collection<Long> participantIds) {
        super(HttpStatus.CONFLICT, "Participants are not free: " + participantIds);
    }
}
