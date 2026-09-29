package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(String email, String password) {
        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        return users.save(new User(null, email, passwordEncoder.encode(password)));
    }

    public String login(String email, String password) {
        User user = users.findByEmail(email)
                .filter(found -> passwordEncoder.matches(password, found.getPassword()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        return jwtService.createToken(user.getEmail());
    }
}