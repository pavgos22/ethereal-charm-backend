package com.ethereal.auth.services;

import com.ethereal.auth.config.EmailConfiguration;
import com.ethereal.auth.entity.User;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfiguration emailConfiguration;

    @Value("${front.url}")
    private String frontendUrl;

    @Value("classpath:/templates/activation-mail.html")
    private Resource activationTemplate;

    @Value("classpath:/templates/password-reset.html")
    private Resource recoveryTemplate;

    @Value("classpath:/static/img/logo.png")
    private Resource logoResource;

    private byte[] logoBytes;

    @PostConstruct
    public void init() {
        try (InputStream is = logoResource.getInputStream()) {
            this.logoBytes = IOUtils.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException("Nie można wczytać logo", e);
        }
    }

    public void sendActivation(User user) {
        log.info("--START sendActivation");
        try {
            String html = readTemplate(activationTemplate)
                    .replace("https://google.com",
                            frontendUrl + "/activate/" + user.getUuid());

            emailConfiguration.sendMailWithInlineImage(
                    user.getEmail(), html, "Aktywacja konta", logoBytes);

        } catch (IOException | MessagingException e) {
            log.error("Błąd wysyłania maila", e);
            throw new RuntimeException(e);
        }
        log.info("--STOP sendActivation");
    }

    public void sendPasswordRecovery(User user, String uid) {
        log.info("--START sendPasswordRecovery");
        try {
            String html = readTemplate(recoveryTemplate)
                    .replace("https://google.com",
                            frontendUrl + "/password-recovery/" + uid);

            emailConfiguration.sendMail(
                    user.getEmail(), html, "Odzyskanie hasła", true);

        } catch (IOException e) {
            log.error("Cannot send mail", e);
            throw new RuntimeException(e);
        }
        log.info("--STOP sendPasswordRecovery");
    }

    public void sendTwoFactorCode(String email, String code) {
        String html = """
        <div style="font-family:Arial,sans-serif">
          <p>Twój kod logowania 2FA:</p>
          <p style="font-size:24px;font-weight:bold;letter-spacing:3px">%s</p>
          <p>Kod wygasa za 5 minut.</p>
        </div>
        """.formatted(code);
        try {
            emailConfiguration.sendMail(email, html, "Kod weryfikacyjny (2FA)", true);
        } catch (Exception e) {
            log.error("Błąd wysyłania 2FA", e);
            throw new RuntimeException(e);
        }
    }

    private static String readTemplate(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}