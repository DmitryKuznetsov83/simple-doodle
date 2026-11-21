package org.example.doodle.calendar.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO temporary skeleton check, remove later
@RestController
public class HelloController {

    private final HelloService service;

    public HelloController(HelloService service) {
        this.service = service;
    }

    @GetMapping("/calendar/hello")
    public String hello() {
        return service.hello();
    }
}
