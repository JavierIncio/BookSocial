package com.booksocial.identity.service;

import com.booksocial.identity.domain.Role;
import com.booksocial.identity.domain.User;
import com.booksocial.identity.dto.UserResponse;
import com.booksocial.identity.repository.UserRepository;
import com.booksocial.identity.dto.RegisterRequest;
import com.booksocial.identity.exception.EmailAlreadyExistsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new EmailAlreadyExistsException();

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(encoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBirthDate(request.birthDate());
        user.setEnabled(true);

        if (user.getAge() < 18)
            user.setRoles(Set.of(Role.MINOR_USER));
        else
            user.setRoles(Set.of(Role.USER));


        userRepository.save(user);
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getAge(), user.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
    }

}
