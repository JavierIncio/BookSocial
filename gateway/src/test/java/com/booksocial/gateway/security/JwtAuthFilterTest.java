package com.booksocial.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class JwtAuthFilterTest {

    private static final String ISSUER = "booksocial-identity";
    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "test-secret-for-gateway-test-keys-0123456789-abcdefghijklmnop"
                    .getBytes(StandardCharsets.UTF_8));

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(new JwtService(TEST_SECRET, ISSUER));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accessToken_injectsUserHeaders_andOverridesSpoofed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "999");
        request.addHeader("Authorization", "Bearer " + token("access", "alice@booksocial.com", 42L,
                List.of("USER", "ADMIN")));

        MockFilterChain chain = runFilter(request);

        HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded.getHeader("X-User-Id")).isEqualTo("42");
        assertThat(forwarded.getHeader("X-User-Email")).isEqualTo("alice@booksocial.com");
        assertThat(forwarded.getHeader("X-User-Roles")).isEqualTo("USER,ADMIN");

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void noToken_stripsSpoofedUserHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "999");
        request.addHeader("X-User-Email", "hacker@evil.com");
        request.addHeader("X-User-Roles", "ADMIN");

        HttpServletRequest forwarded = (HttpServletRequest) runFilter(request).getRequest();

        assertThat(forwarded.getHeader("X-User-Id")).isNull();
        assertThat(forwarded.getHeader("X-User-Email")).isNull();
        assertThat(forwarded.getHeader("X-User-Roles")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void refreshToken_isNotAuthenticated_andNoUserHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token("refresh", "alice@booksocial.com", 42L,
                List.of("USER")));

        HttpServletRequest forwarded = (HttpServletRequest) runFilter(request).getRequest();

        assertThat(forwarded.getHeader("X-User-Id")).isNull();
        assertThat(forwarded.getHeader("X-User-Email")).isNull();
        assertThat(forwarded.getHeader("X-User-Roles")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidToken_isNotAuthenticated_andNoUserHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not.a.jwt");

        HttpServletRequest forwarded = (HttpServletRequest) runFilter(request).getRequest();

        assertThat(forwarded.getHeader("X-User-Id")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void accessTokenWithoutRoles_isNotAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token("access", "alice@booksocial.com", 42L,
                List.of()));

        HttpServletRequest forwarded = (HttpServletRequest) runFilter(request).getRequest();

        assertThat(forwarded.getHeader("X-User-Id")).isNull();
        assertThat(forwarded.getHeader("X-User-Roles")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockFilterChain runFilter(MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    private String token(String type, String email, Long uid, List<String> roles) {
        return Jwts.builder()
                .subject(email)
                .claim("uid", uid)
                .claim("roles", roles)
                .claim("type", type)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
    }

}
