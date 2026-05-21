package com.kncn.jwtauth.web;

import com.kncn.jwtauth.dto.AuthDtos.*;
import com.kncn.jwtauth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        try {
            return ResponseEntity.ok(auth.register(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(409).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(auth.login(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            return ResponseEntity.ok(auth.refresh(req.refreshToken()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(Authentication a) {
        if (a != null) auth.logout(a.getName());
        return ResponseEntity.ok(new MessageResponse("logged out"));
    }
}
