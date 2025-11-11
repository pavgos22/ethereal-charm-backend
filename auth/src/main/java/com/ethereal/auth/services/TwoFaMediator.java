package com.ethereal.auth.services;

import com.ethereal.auth.entity.User;
import com.ethereal.auth.entity.UserRegisterDTO;
import com.ethereal.auth.exceptions.UserDoesntExistException;
import com.ethereal.auth.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFaMediator {

    private final TwoFaService twoFaService;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final UserRepository userRepository;

    @Value("${jwt.exp}")
    private int exp;

    @Value("${jwt.refresh-exp}")
    private int refreshExp;

    public UserRegisterDTO verifyAndIssueTokens(String challengeId, String code, HttpServletResponse response) {
        var user = twoFaService.verifyLoginChallenge(UUID.fromString(challengeId), code);

        Cookie refresh = cookieService.generateCookie("refresh",
                jwtService.generateToken(user.getUsername(), refreshExp), refreshExp);
        Cookie auth = cookieService.generateCookie("Authorization",
                jwtService.generateToken(user.getUsername(), exp), exp);
        response.addCookie(auth);
        response.addCookie(refresh);

        return UserRegisterDTO.builder()
                .login(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public void toggleTwoFactor(HttpServletRequest request, boolean enabled) {
        String subject = extractSubjectFromCookies(request);
        User user = userRepository.findUserByLoginOrEmailAndLockAndEnabled(subject)
                .orElseThrow(() -> new UserDoesntExistException("User with username/email " + subject + " does not exist"));
        user.setTwoFactorEnabled(enabled);
        userRepository.save(user);
    }

    private String extractSubjectFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) throw new IllegalArgumentException("Token not found in request");

        String token = Arrays.stream(cookies)
                .filter(c -> "Authorization".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Token not found in request"));

        return jwtService.getSubject(token);
    }
}