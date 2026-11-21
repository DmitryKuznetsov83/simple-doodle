package org.example.doodle.booking;

import org.example.doodle.core.projection.GuavaProjectionBuilder;
import org.example.doodle.core.projection.ProjectionBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectionConfig {

    @Bean
    public ProjectionBuilder projectionBuilder() {
        return new GuavaProjectionBuilder();
    }
}
