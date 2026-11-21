package org.example.doodle.booking.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank @Size(max = 255) String name) {
}
