package org.example.doodle.booking.event;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface EventRepository extends ListCrudRepository<EventEntity, Long> {

    /**
     * Events of the users overlapping {@code [startTime, endTime)}, in log order.
     */
    @Query("""
            SELECT * FROM event
            WHERE user_id IN (:userIds)
              AND start_time < :endTime
              AND end_time > :startTime
            ORDER BY id""")
    List<EventEntity> findOverlapping(Collection<Long> userIds, LocalDateTime startTime, LocalDateTime endTime);
}
