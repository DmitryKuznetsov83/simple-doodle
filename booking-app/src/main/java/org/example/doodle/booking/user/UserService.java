package org.example.doodle.booking.user;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User create(String name) {
        try {
            return repository.save(new User(null, name));
        } catch (DuplicateKeyException e) {
            // unique constraint users_name_uk
            throw new UserAlreadyExistsException(name);
        }
    }
}
