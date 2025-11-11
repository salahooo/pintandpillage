package nl.duckstudios.pintandpillage.dao;

import nl.duckstudios.pintandpillage.entity.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserDAO {

    private final UserRepository userRepository;

    public UserDAO(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUsername(String username) { // REFACTOR (ITSTEN H2): Expose username lookup for combined email/username login.
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        return this.userRepository.save(user);
    }

    public Optional<User> findUsernameById(long userId) {
        return this.userRepository.findById(userId);
    }
}
