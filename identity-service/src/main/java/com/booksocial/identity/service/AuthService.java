package com.booksocial.identity.service;

import com.booksocial.identity.domain.User;
import com.booksocial.identity.dto.LoginRequest;
import com.booksocial.identity.dto.RegisterRequest;
import com.booksocial.identity.dto.TokenResponse;
import com.booksocial.identity.exception.InvalidRefreshTokenException;
import com.booksocial.identity.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserService userService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    public TokenResponse register(RegisterRequest request) {
        return issueTokens(userService.register(request));
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userService.findByEmail(request.email()).orElseThrow();
        return issueTokens(user);
    }

    public TokenResponse refresh(String rawToken) {
        Claims claims = jwtService.parse(rawToken);
        if(!JwtService.TYPE_REFRESH.equals(claims.get("type",  String.class)))
            throw new InvalidRefreshTokenException();
        if(!refreshTokenService.isValid(rawToken))
            throw new InvalidRefreshTokenException();

        User user = userService.findByEmail(claims.getSubject()).orElseThrow();
        refreshTokenService.revoke(rawToken);
        return issueTokens(user);
    }

    public void logout(String rawToken) {
        refreshTokenService.revoke(rawToken);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.store(user, refreshToken, Instant.now().plus(jwtService.refreshTokenTtl()));
        return new TokenResponse(accessToken, refreshToken,
                jwtService.accessTokenTtl().toSeconds(), "Bearer");
    }
}
