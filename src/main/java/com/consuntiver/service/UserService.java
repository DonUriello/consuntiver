package com.consuntiver.service;

import com.consuntiver.model.User;
import com.consuntiver.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuovo utente con password cifrata (BCrypt). L'email e' facoltativa.
     *
     * @throws IllegalArgumentException se lo username e' gia' in uso.
     */
    public User register(String username, String rawPassword, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username gia' in uso");
        }
        String normalizedEmail = (email != null && !email.isBlank()) ? email.trim() : null;
        User user = new User(username, passwordEncoder.encode(rawPassword), normalizedEmail);
        return userRepository.save(user);
    }
}
