package com.example.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    void registerStoresEncodedPassword() {
        when(users.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = authService.register("user@example.com", "password");

        assertEquals("bcrypt-hash", user.getPassword());
        verify(users).save(any(User.class));
    }

    @Test
    void registerRejectsExistingEmail() {
        when(users.existsByEmail("user@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register("user@example.com", "password"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void registerMapsDuplicateKeyRaceToConflict() {
        when(users.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("bcrypt-hash");
        when(users.save(any(User.class))).thenThrow(new DuplicateKeyException("duplicate"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register("user@example.com", "password"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void loginReturnsIssuedTokenForMatchingPassword() {
        User user = new User("id", "user@example.com", "bcrypt-hash");
        when(users.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password", "bcrypt-hash")).thenReturn(true);
        when(jwtService.createToken("user@example.com")).thenReturn("signed-token");

        assertEquals("signed-token", authService.login("user@example.com", "password"));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("id", "user@example.com", "bcrypt-hash");
        when(users.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("user@example.com", "wrong"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(users.findByEmail("missing@example.com")).thenReturn(java.util.Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login("missing@example.com", "password"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}