package com.booksocial.identity.config;

import com.booksocial.identity.domain.Role;
import com.booksocial.identity.domain.User;
import com.booksocial.identity.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class AdminDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public AdminDataInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder, Environment environment) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.existsByEmail(environment.getProperty("app.admin.email"))) return;

        User admin = new User();
        admin.setEmail(environment.getProperty("app.admin.email"));
        admin.setPasswordHash(passwordEncoder.encode(environment.getProperty("app.admin.password")));
        admin.setFirstName("Administrator");
        admin.setBirthDate(LocalDate.of(1990, 1, 1));
        admin.setRoles(Set.of(Role.ADMIN, Role.USER));
        admin.setEnabled(true);

        userRepository.save(admin);
    }
}
