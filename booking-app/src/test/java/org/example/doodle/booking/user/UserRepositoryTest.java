package org.example.doodle.booking.user;

import org.example.doodle.booking.PostgresTestcontainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainer.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void locksExistingUsersInIdOrder() {
        long alice = userRepository.save(new User(null, "alice")).id();
        long bob = userRepository.save(new User(null, "bob")).id();
        long missing = bob + 1000;

        List<Long> locked = userRepository.lockAllById(List.of(missing, bob, alice));

        assertThat(locked).containsExactly(alice, bob);
    }
}
