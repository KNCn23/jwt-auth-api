package com.kncn.jwtauth.service;

import com.kncn.jwtauth.domain.RefreshToken;
import com.kncn.jwtauth.domain.Role;
import com.kncn.jwtauth.domain.User;
import com.kncn.jwtauth.dto.AuthDtos.*;
import com.kncn.jwtauth.repo.RefreshTokenRepository;
import com.kncn.jwtauth.repo.UserRepository;
import com.kncn.jwtauth.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refresh;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshExpirationMs;

    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users,
                       RefreshTokenRepository refresh,
                       PasswordEncoder encoder,
                       JwtService jwt) {
        this.users = users; this.refresh = refresh;
        this.encoder = encoder; this.jwt = jwt;
    }

    public TokenResponse register(RegisterRequest req) {
        if (users.existsByUsername(req.username()))
            throw new IllegalArgumentException("username already taken");
        User u = new User(req.username(),
                          encoder.encode(req.password()),
                          Set.of(Role.USER));
        users.save(u);
        return issueTokens(u);
    }

    public TokenResponse login(LoginRequest req) {
        User u = users.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("bad credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash()))
            throw new IllegalArgumentException("bad credentials");
        return issueTokens(u);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenStr) {
        RefreshToken rt = refresh.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("unknown refresh token"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now()))
            throw new IllegalArgumentException("expired refresh token");
        /* Rotate: revoke the old token, issue a new pair. */
        rt.setRevoked(true);
        refresh.save(rt);
        return issueTokens(rt.getUser());
    }

    @Transactional
    public void logout(String username) {
        users.findByUsername(username).ifPresent(u ->
            refresh.revokeAllForUser(u.getId()));
    }

    private TokenResponse issueTokens(User u) {
        String access  = jwt.generateAccessToken(u);
        String refreshStr = generateRefreshToken();
        RefreshToken rt = new RefreshToken(u, refreshStr,
                Instant.now().plusMillis(refreshExpirationMs));
        refresh.save(rt);
        return new TokenResponse(access, refreshStr, jwt.getAccessExpirationSeconds());
    }

    private String generateRefreshToken() {
        byte[] buf = new byte[48];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
