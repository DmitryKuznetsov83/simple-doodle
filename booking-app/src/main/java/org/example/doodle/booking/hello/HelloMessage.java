package org.example.doodle.booking.hello;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

// TODO temporary skeleton check, remove later
@Table("temp_table")
public record HelloMessage(@Id String value) {
}
