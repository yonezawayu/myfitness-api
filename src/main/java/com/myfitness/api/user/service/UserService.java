package com.myfitness.api.user.service;

import com.myfitness.api.user.dto.UserCreateRequest;
import com.myfitness.api.user.dto.UserResponse;
import com.myfitness.api.user.entity.User;
import com.myfitness.api.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(UserCreateRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }

        String hash = passwordEncoder.encode(req.password());
        User user = new User(email, hash, "USER");
        User saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getEmail(), saved.getRole());
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole());
    }
}