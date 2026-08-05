package com.booksocial.identity.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class TokenCookieService {

    private final JwtService jwtService;

    public TokenCookieService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.refreshTokenTtl().getSeconds())
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
