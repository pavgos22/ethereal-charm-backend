package com.ethereal.order.config;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Configuration
@Slf4j
public class EmailConfiguration {

    private final String email;
    private final String password;
    private final String smtpHost;
    private final int smtpPort;
    private final boolean sslEnabled;
    private final boolean startTlsEnabled;

    private Authenticator auth;
    private Session session;
    private Properties properties;

    public EmailConfiguration(
            @Value("${notification.mail}") String email,
            @Value("${notification.password}") String password,
            @Value("${notification.smtp.host}") String smtpHost,
            @Value("${notification.smtp.port}") int smtpPort,
            @Value("${notification.smtp.ssl}") boolean sslEnabled,
            @Value("${notification.smtp.starttls}") boolean startTlsEnabled
    ) {
        this.email = email;
        this.password = password;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.sslEnabled = sslEnabled;
        this.startTlsEnabled = startTlsEnabled;
        config();
    }

    private void config() {
        properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        if (sslEnabled) {
            properties.put("mail.smtp.socketFactory.port", smtpPort);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.put("mail.smtp.socketFactory.fallback", "false");
        }

        if (startTlsEnabled) {
            properties.put("mail.smtp.starttls.enable", "true");
        }

        this.auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        };
    }

    private void refreshSession() {
        session = Session.getInstance(properties, auth);
    }

    public void sendMail(String recipientEmail, String content, String subject, boolean onCreate) {
        if (session == null)
            refreshSession();

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(String.format("\"%s\" <%s>", "Ethereal Charm", email)));
            if (properties.getProperty("mail.smtp.from") == null) properties.put("mail.smtp.from", email);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(content, "text/html; charset=utf-8");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
            Transport.send(message);
        } catch (MessagingException e) {
            log.error("Mail send failed", e);
            if (onCreate) {
                refreshSession();
                sendMail(recipientEmail, content, subject, false);
            }
        }
    }
}