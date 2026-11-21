package org.example.doodle.calendar;

import org.example.doodle.calendar.meeting.MeetingRepository;
import org.example.doodle.calendar.user.UserCalendarRepository;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository test on {@link PostgresTestcontainer}. All repository tests share the same Spring context,
 * hence the same container.
 * <p>
 * Test data is inserted within the test transaction and rolled back, so the datasource is not read-only here.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@JdbcTest(properties = "spring.datasource.hikari.read-only=false")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainer.class, UserCalendarRepository.class, MeetingRepository.class})
public @interface RepositoryTest {
}
