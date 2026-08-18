package com.booksocial.identity.config;

import com.booksocial.identity.domain.Role;
import com.booksocial.identity.domain.User;
import com.booksocial.identity.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class AdminDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminDataInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder, AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.existsByEmail(adminProperties.email())) return;

        User admin = new User();
        admin.setEmail(adminProperties.email());
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.password()));
        admin.setFirstName("Administrator");
        admin.setBirthDate(LocalDate.of(1990, 1, 1));
        admin.setRoles(Set.of(Role.ADMIN, Role.USER));
        admin.setEnabled(true);

        userRepository.save(admin);
    }
}