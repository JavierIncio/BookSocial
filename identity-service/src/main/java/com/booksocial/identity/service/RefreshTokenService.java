package com.booksocial.identity.service;

import com.booksocial.identity.domain.RefreshToken;
import com.booksocial.identity.domain.User;
import com.booksocial.identity.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    public void store(User user, String rawToken, Instant expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash(rawToken));
        rt.setExpiresAt(expiresAt);
        rt.setRevoked(false);
        rt.setCreatedAt(Instant.now());
        repository.save(rt);
    }

    public boolean isValid(String rawToken) {
        return repository.findByTokenHash(hash(rawToken))
                .map(rt -> !rt.isRevoked() && rt.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }

    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    repository.save(rt);
                });
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
