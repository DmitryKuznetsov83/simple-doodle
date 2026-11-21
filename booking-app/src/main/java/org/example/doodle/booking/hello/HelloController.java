package org.example.doodle.booking.hello;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO temporary skeleton check, remove later
@RestController
public class HelloController {

    private final HelloService service;

    public HelloController(HelloService service) {
        this.service = service;
    }

    @GetMapping("/booking/hello")
    public String hello() {
        return service.hello();
    }
}
