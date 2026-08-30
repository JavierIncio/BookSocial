package com.booksocial.identity.service;

import com.booksocial.identity.domain.PasswordResetToken;
import com.booksocial.identity.domain.User;
import com.booksocial.identity.exception.AlreadyUsedTokenException;
import com.booksocial.identity.exception.ExpiredTokenException;
import com.booksocial.identity.exception.InvalidTokenException;
import com.booksocial.identity.repository.PasswordResetTokenRepository;
import com.booksocial.identity.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
public class PasswordResetService {
    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;

    private final String from;
    private final String resetBaseUrl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder encoder,
                                JavaMailSender mailSender,
                                @Value("${app.mail.from}") String from,
                                @Value("${app.mail.reset-base-url}") String resetBaseUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.mailSender = mailSender;
        this.from = from;
        this.resetBaseUrl = resetBaseUrl;
    }

    public void requestReset(String email) {
        // Siempre termina sin error, aunque el email no exista (no revelar existencia).
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isEmpty()) return;

        User user = byEmail.get();
        tokenRepository.deleteByUserId(user.getId());

        String raw = generateToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        token.setUsed(false);
        tokenRepository.save(token);

        sendEmail(user.getEmail(), raw);
    }

    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("INVALID_TOKEN"));
        if (token.isUsed()) throw new AlreadyUsedTokenException("ALREADY_USED");
        if (token.getExpiresAt().isBefore(Instant.now())) throw new ExpiredTokenException("EXPIRED_TOKEN");

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidTokenException("INVALID_TOKEN"));

        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);
    }

    private void sendEmail(String to, String rawToken) {
        try {
            String resetUrl = resetBaseUrl + "/reset-password?token=" + rawToken;
            ClassPathResource resource = new ClassPathResource("templates/password-reset-email.html");
            String html = StreamUtils.copyToString(resource.getInputStream(),
                    StandardCharsets.UTF_8).replace("{{RESET_URL}}", resetUrl);

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("BookSocial - Reset your password");
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send reset email.", e);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
