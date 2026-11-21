package org.example.doodle.calendar.user;

import org.example.doodle.core.time.TimeRange;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/calendar/users/{userId}")
public class UserCalendarController {

    private final UserCalendarService service;

    public UserCalendarController(UserCalendarService service) {
        this.service = service;
    }

    @GetMapping
    public UserCalendarResponse calendar(
            @PathVariable long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return service.calendar(userId, new TimeRange(startTime, endTime));
    }
}
