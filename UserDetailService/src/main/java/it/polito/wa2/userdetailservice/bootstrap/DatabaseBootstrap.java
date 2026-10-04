package it.polito.wa2.userdetailservice.bootstrap;

import it.polito.wa2.userdetailservice.entities.User;
import it.polito.wa2.userdetailservice.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DatabaseBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(DatabaseBootstrap.class);
    private final String seedEmail = "mario.rossi@example.com";

    public DatabaseBootstrap(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        User user = userRepository.findByEmail(seedEmail);
        if (user != null) {
            UUID id = user.getId();
            logger.info("Initial user id: {}", id);
        } else {
            logger.warn("Seed user with email {} not found in DB", seedEmail);
        }
    }
}
