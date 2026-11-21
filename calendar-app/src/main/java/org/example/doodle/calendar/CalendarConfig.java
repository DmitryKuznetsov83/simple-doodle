package org.example.doodle.calendar;

import org.example.doodle.core.projection.GuavaProjectionBuilder;
import org.example.doodle.core.projection.ProjectionBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CalendarProperties.class)
public class CalendarConfig {

    @Bean
    public ProjectionBuilder projectionBuilder() {
        return new GuavaProjectionBuilder();
    }
}
