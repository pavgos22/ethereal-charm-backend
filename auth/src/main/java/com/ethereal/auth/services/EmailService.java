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

    private static String readTemplate(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}