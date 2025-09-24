package com.documentmanager.controller;

import com.documentmanager.dto.AuthRequest;
import com.documentmanager.dto.AuthResponse;
import com.documentmanager.dto.RegisterRequest;
import com.documentmanager.entity.User;
import com.documentmanager.service.AuthService;
import com.documentmanager.service.UserService;
import com.documentmanager.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = jwtUtil.generateToken(request.getUsername());
            User user = userService.findByUsername(request.getUsername());

            AuthResponse response = new AuthResponse(token, user.getId(), user.getUsername(),
                    user.getEmail(), user.getFirstName(), user.getLastName());
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (userService.existsByUsername(request.getUsername())) {
                return ResponseEntity.badRequest().body("Username is already taken!");
            }

            if (userService.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Email is already in use!");
            }

            User user = authService.registerUser(request);
            String token = jwtUtil.generateToken(user.getUsername());

            AuthResponse response = new AuthResponse(token, user.getId(), user.getUsername(),
                    user.getEmail(), user.getFirstName(), user.getLastName());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating user: " + e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(jwt);

            if (jwtUtil.validateToken(jwt, username)) {
                User user = userService.findByUsername(username);
                return ResponseEntity.ok(new AuthResponse(jwt, user.getId(), user.getUsername(),
                        user.getEmail(), user.getFirstName(), user.getLastName()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }
}