package org.example.doodle.booking.event;

import org.springframework.data.repository.ListCrudRepository;

public interface EventRepository extends ListCrudRepository<EventEntity, Long> {
}
