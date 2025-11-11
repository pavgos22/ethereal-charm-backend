package com.ethereal.auth.services;

import com.ethereal.auth.entity.AuthChallenge;
import com.ethereal.auth.entity.User;
import com.ethereal.auth.repository.AuthChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TwoFaService {

    private final AuthChallengeRepository repo;
    private final EmailService emailService;

    @Value("${twofa.otp.length:6}")
    private int otpLength;

    @Value("${twofa.otp.ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${twofa.otp.max-attempts:5}")
    private int maxAttempts;

    public UUID createLoginChallenge(User user) {
        String code = generateCode(otpLength);
        AuthChallenge ch = new AuthChallenge();
        ch.setId(UUID.randomUUID());
        ch.setUser(user);
        ch.setPurpose("LOGIN_2FA");
        ch.setCodeHash(DigestUtils.sha256Hex(code));
        ch.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));
        ch.setAttemptsLeft(maxAttempts);
        repo.save(ch);

        emailService.sendTwoFactorCode(user.getEmail(), code);
        return ch.getId();
    }

    public User verifyLoginChallenge(UUID id, String code) {
        AuthChallenge ch = repo.findByIdAndConsumedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid challenge"));
        if (Instant.now().isAfter(ch.getExpiresAt())) {
            throw new IllegalStateException("Expired");
        }
        if (ch.getAttemptsLeft() <= 0) {
            throw new IllegalStateException("Too many attempts");
        }
        boolean ok = DigestUtils.sha256Hex(code).equals(ch.getCodeHash());
        if (!ok) {
            ch.setAttemptsLeft(ch.getAttemptsLeft() - 1);
            repo.save(ch);
            throw new IllegalArgumentException("Invalid code");
        }
        ch.setConsumedAt(Instant.now());
        repo.save(ch);
        return ch.getUser();
    }

    private static String generateCode(int len) {
        int n = ThreadLocalRandom.current().nextInt((int) Math.pow(10, len));
        return String.format("%0" + len + "d", n);
    }
}