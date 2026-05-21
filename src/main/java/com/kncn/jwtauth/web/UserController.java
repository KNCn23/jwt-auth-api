package com.kncn.jwtauth.web;

import com.kncn.jwtauth.dto.AuthDtos.MessageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of(
            "username", auth.getName(),
            "authorities", auth.getAuthorities());
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse adminOnly() {
        return new MessageResponse("hello, admin");
    }
}
