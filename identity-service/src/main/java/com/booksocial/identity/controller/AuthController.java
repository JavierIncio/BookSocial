package com.booksocial.identity.controller;

import com.booksocial.identity.dto.*;
import com.booksocial.identity.exception.InvalidRefreshTokenException;
import com.booksocial.identity.security.TokenCookieService;
import com.booksocial.identity.service.AuthService;
import com.booksocial.identity.service.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieService cookieService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          TokenCookieService cookieService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        TokenResponse tokens = authService.register(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(tokens.refreshToken()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }


    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokens = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(tokens.refreshToken()).toString());
        return tokens;
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody(required = false) RefreshRequest request,
                                 @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
                                 HttpServletResponse response) {
        String refreshToken = refreshTokenCookie != null
                ? refreshTokenCookie
                : (request != null ? request.refreshToken() : null);
        if (refreshToken == null)
            throw new InvalidRefreshTokenException();

        TokenResponse tokens = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(tokens.refreshToken()).toString());
        return tokens;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                       @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
                                       HttpServletResponse response) {
        String refreshToken = refreshTokenCookie != null
                ? refreshTokenCookie
                : (request != null ? request.refreshToken() : null);
        if (refreshToken != null)
            authService.logout(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
