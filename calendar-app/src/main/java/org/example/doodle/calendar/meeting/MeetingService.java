package org.example.doodle.calendar.meeting;

import org.example.doodle.calendar.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository repository;

    public MeetingService(MeetingRepository repository) {
        this.repository = repository;
    }

    /**
     * The meeting: organizer, time and participants are taken from its events.
     */
    @Transactional(readOnly = true)
    public MeetingResponse meeting(long meetingId) {
        List<MeetingRow> rows = repository.findMeeting(meetingId);
        if (rows.isEmpty()) {
            throw new MeetingNotFoundException(meetingId);
        }

        MeetingRow first = rows.getFirst();
        List<User> participants = rows.stream()
                .map(row -> new User(row.participantId(), row.participantName()))
                .toList();

        return new MeetingResponse(first.meetingId(), first.title(), first.description(),
                new User(first.organizerId(), first.organizerName()),
                first.startTime(), first.endTime(), participants);
    }
}
