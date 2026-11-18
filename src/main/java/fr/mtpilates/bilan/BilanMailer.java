package fr.mtpilates.bilan;

import fr.mtpilates.auth.AuthProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/** Email d'envoi d'un bilan : PDF en pièce jointe et lien vers l'espace de la membre. */
@Component
public class BilanMailer {

    static final String SUBJECT = "Ton bilan de transformation — MT Pilates";
    private static final Logger LOG = LoggerFactory.getLogger(BilanMailer.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templates;
    private final AuthProperties properties;

    public BilanMailer(final JavaMailSender mailSender, final SpringTemplateEngine templates, final AuthProperties properties) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.properties = properties;
    }

    /** Faux si l'email n'a pas pu partir : le bilan reste disponible dans l'espace de la membre. */
    public boolean send(final String email, final String firstName, final String filename, final byte[] pdf, final String appLink) {
        final Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("link", appLink);
        try {
            final MimeMessage message = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.mailFrom());
            helper.setTo(email);
            helper.setSubject(SUBJECT);
            helper.setText(templates.process("mail/bilan", context), true);
            helper.addAttachment(filename, new ByteArrayResource(pdf), "application/pdf");
            mailSender.send(message);
            return true;
        } catch (final MessagingException | MailException e) {
            LOG.warn("Email du bilan non envoyé : {}", e.getMessage());
            return false;
        }
    }
}
