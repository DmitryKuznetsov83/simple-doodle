package org.example.doodle.calendar;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * @param maxWindow max length of the window of a user's calendar
 */
@ConfigurationProperties("calendar")
public record CalendarProperties(@DefaultValue("P31D") Duration maxWindow) {
}
