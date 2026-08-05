package com.booksocial.identity.security;

import com.booksocial.identity.domain.User;
import com.booksocial.identity.dto.TokenResponse;
import com.booksocial.identity.service.AuthService;
import com.booksocial.identity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final AuthService authService;
    private final TokenCookieService cookieService;

    @Value("${app.oauth2.frontend-redirect-uri}")
    private String frontendRedirect;


    public OAuth2AuthenticationSuccessHandler(UserService userService,
                                              AuthService authService,
                                              TokenCookieService cookieService) {
        this.userService = userService;
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        User user = userService.linkOrCreateOAuthUser(oauth2User.getAttributes());
        TokenResponse tokens = authService.issueTokens(user);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(tokens.refreshToken()).toString());
        response.sendRedirect(frontendRedirect + "#access_token=" + tokens.accessToken());
    }
}
