package com.ethereal.auth.services;

import com.ethereal.auth.config.EmailConfiguration;
import com.ethereal.auth.entity.User;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfiguration emailConfiguration;

    @Value("${front.url}")
    private String fontendUrl;

    @Value("classpath:static/mail-aktywuj.html")
    private Resource activeTemplate;
    @Value("classpath:static/resetuj-haslo.html")
    private Resource recoveryTemplate;

    public void sendActivation(User user) {
        log.info("--START sendActivation");
        try {
            String html = Files.toString(activeTemplate.getFile(), Charsets.UTF_8);
            html = html.replace("https://google.com", fontendUrl + "/activate/" + user.getUuid());
            emailConfiguration.sendMail(user.getEmail(), html, "Aktywacja konta", true);
        } catch (IOException e) {
            log.info("Cannot send mail");
            throw new RuntimeException(e);
        }
        log.info("--STOP sendActivation");
    }

    public void sendPasswordRecovery(User user, String uid) {
        try {
            log.info("--START sendPasswordRecovery");
            String html = Files.toString(recoveryTemplate.getFile(), Charsets.UTF_8);
            html = html.replace("https://google.com", fontendUrl + "/password-recovery/" + uid);
            emailConfiguration.sendMail(user.getEmail(), html, "Odzyskanie hasła", true);
        } catch (IOException e) {
            log.info("Cannot send mail");
            throw new RuntimeException(e);
        }
        log.info("--STOP sendPasswordRecovery");
    }
}
