package org.example.doodle.booking.user;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Collection;
import java.util.List;

public interface UserRepository extends ListCrudRepository<User, Long> {

    /**
     * Locks the users' rows until the end of the transaction. Rows are locked in {@code id} order,
     * the same in every transaction, so concurrent commands do not deadlock.
     *
     * @return ids of the existing (and now locked) users, in ascending order
     */
    @Query("SELECT id FROM users WHERE id IN (:ids) ORDER BY id FOR UPDATE")
    List<Long> lockAllById(Collection<Long> ids);
}
