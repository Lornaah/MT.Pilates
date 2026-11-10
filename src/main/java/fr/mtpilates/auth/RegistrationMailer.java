package fr.mtpilates.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Envoi de l'email de confirmation d'inscription. */
@Component
public class RegistrationMailer {

    static final String SUBJECT = "Confirmez votre adresse email — MT Pilates";

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templates;
    private final AuthProperties properties;

    public RegistrationMailer(final JavaMailSender mailSender, final SpringTemplateEngine templates,
                              final AuthProperties properties) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.properties = properties;
    }

    public void sendConfirmation(final String email, final String confirmationLink) {
        final Context context = new Context();
        context.setVariable("confirmationLink", confirmationLink);
        final String html = templates.process("mail/confirm-registration", context);
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.mailFrom());
            helper.setTo(email);
            helper.setSubject(SUBJECT);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (final MessagingException e) {
            throw new IllegalStateException("Préparation de l'email de confirmation impossible", e);
        }
    }
}
