package org.example.doodle.core;

/**
 * Thrown when a domain object is created with values that violate its invariants.
 */
public class DomainValidationException extends IllegalArgumentException {

    public DomainValidationException(String message) {
        super(message);
    }
}
