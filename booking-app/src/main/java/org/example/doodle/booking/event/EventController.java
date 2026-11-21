package org.example.doodle.booking.event;

import jakarta.validation.Valid;
import org.example.doodle.core.event.EventType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/booking/users/{userId}")
public class EventController {

    private final EventService service;

    public EventController(EventService service) {
        this.service = service;
    }

    @PostMapping("/free")
    @ResponseStatus(HttpStatus.CREATED)
    public EventEntity markFree(@PathVariable long userId, @Valid @RequestBody TimeRangeRequest request) {
        return service.mark(userId, EventType.MARKED_FREE, request.toTimeRange());
    }

    @PostMapping("/busy")
    @ResponseStatus(HttpStatus.CREATED)
    public EventEntity markBusy(@PathVariable long userId, @Valid @RequestBody TimeRangeRequest request) {
        return service.mark(userId, EventType.MARKED_BUSY, request.toTimeRange());
    }
}
