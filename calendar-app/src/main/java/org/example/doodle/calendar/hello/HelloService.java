package org.example.doodle.calendar.hello;

import org.springframework.stereotype.Service;

// TODO temporary skeleton check, remove later
@Service
public class HelloService {

    private final HelloRepository repository;

    public HelloService(HelloRepository repository) {
        this.repository = repository;
    }

    public String hello() {
        return repository.findAll().stream()
                .findFirst()
                .map(HelloMessage::value)
                .orElseThrow(() -> new IllegalStateException("temp_table is empty"));
    }
}
