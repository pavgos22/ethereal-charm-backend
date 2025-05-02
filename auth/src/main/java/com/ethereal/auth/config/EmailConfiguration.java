package com.ethereal.auth.config;

import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@Slf4j
public class EmailConfiguration {

    private final String email;
    private final String password;
    private Authenticator auth;
    private Session session;
    private Properties properties;

    public EmailConfiguration(@Value("${notification.mail}") String email, @Value("${notification.password}") String password) {
        this.email = email;
        this.password = password;
        config();
    }

    private void config() {
        String smtpHost = "smtp.gmail.com";
        int smtpPort = 587;

        properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", smtpHost);
        properties.put("mail.smtp.port", smtpPort);

        this.auth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        };
    }

    private void refreshSession() {
        session = Session.getInstance(properties, auth);
    }

    private void refreshSessionIfNeeded() {
        if (session == null)
            refreshSession();
    }

    public void sendMailWithInlineImage(String to,
                                        String html,
                                        String subject,
                                        byte[] imageBytes) throws MessagingException {

        if (session == null) {
            refreshSession();
        }

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(email));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject, StandardCharsets.UTF_8.name());

        ByteArrayDataSource ds = new ByteArrayDataSource(imageBytes, "image/png");
        ds.setName("logo.png");

        MimeBodyPart imgPart = new MimeBodyPart();
        imgPart.setDataHandler(new DataHandler(ds));
        imgPart.setHeader("Content-Type", "image/png; name=logo.png");
        imgPart.setFileName("logo.png");
        imgPart.setContentID("<logo>");
        imgPart.setDisposition(MimeBodyPart.INLINE);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Wersja tekstowa wiadomości", StandardCharsets.UTF_8.name());

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");

        MimeMultipart alternative = new MimeMultipart("alternative");
        alternative.addBodyPart(textPart);
        alternative.addBodyPart(htmlPart);

        MimeBodyPart altWrapper = new MimeBodyPart();
        altWrapper.setContent(alternative);

        MimeMultipart related = new MimeMultipart("related");
        related.addBodyPart(altWrapper);
        related.addBodyPart(imgPart);

        MimeBodyPart relatedWrapper = new MimeBodyPart();
        relatedWrapper.setContent(related);

        String ct = related.getContentType();

        relatedWrapper.setHeader("Content-Type",
                ct + "; type=\"text/html\"");

        MimeMultipart mixed = new MimeMultipart("mixed");
        mixed.addBodyPart(relatedWrapper);

        msg.setContent(mixed);
        Transport.send(msg);

//        try (FileOutputStream fos = new FileOutputStream("test-mail.eml")) {
//            msg.writeTo(fos);
//        } catch (IOException ex) {
//            log.warn("Failed to save test-mail.eml", ex);
//        }
    }


    public void sendMail(String recipientEmail, String content, String subject, boolean onCreate) {
        if (session == null)
            refreshSession();

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(content, "text/html; charset=utf-8");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            if (onCreate) {
                refreshSession();
                sendMail(recipientEmail, content, subject, false);
            }
        }
    }
}
