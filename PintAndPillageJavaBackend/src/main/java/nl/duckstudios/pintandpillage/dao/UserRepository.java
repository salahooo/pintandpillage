package nl.duckstudios.pintandpillage.dao;

import nl.duckstudios.pintandpillage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username); // REFACTOR (ITSTEN H2): Allow username lookups for login flexibility.
}
