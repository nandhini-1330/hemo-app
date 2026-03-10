package com.anemia.controller;

import com.anemia.model.User;
import com.anemia.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toMap(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@AuthenticationPrincipal User user,
                                       @RequestBody UpdateRequest req) {
        user.setFirstName(req.firstName);
        user.setLastName(req.lastName);
        userRepository.save(user);
        return ResponseEntity.ok(toMap(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user,
                                             @RequestBody PasswordRequest req) {
        if (!passwordEncoder.matches(req.currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }
        user.setPassword(passwordEncoder.encode(req.newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    private Map<String, Object> toMap(User u) {
        return Map.of(
            "id", u.getId(),
            "firstName", u.getFirstName(),
            "lastName", u.getLastName(),
            "email", u.getEmail(),
            "role", u.getRole().name(),
            "createdAt", u.getCreatedAt().toString()
        );
    }

    static class UpdateRequest {
        public String firstName;
        public String lastName;
    }

    static class PasswordRequest {
        public String currentPassword;
        public String newPassword;
    }
}