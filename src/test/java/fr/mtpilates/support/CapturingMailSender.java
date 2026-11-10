package fr.mtpilates.support;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Remplace l'envoi SMTP pendant les tests : les emails sont gardés en mémoire. */
public class CapturingMailSender extends JavaMailSenderImpl {

    private static final Pattern CONFIRMATION_LINK = Pattern.compile("href=\"([^\"]*/auth/confirm\\?token=[^\"]+)\"");

    private final List<MimeMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(final MimeMessage... messages) {
        sent.addAll(List.of(messages));
    }

    public List<MimeMessage> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }

    /** Lien de confirmation du dernier email envoyé. */
    public String lastConfirmationLink() throws MessagingException, IOException {
        final String html = (String) sent.get(sent.size() - 1).getContent();
        final Matcher matcher = CONFIRMATION_LINK.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("Aucun lien de confirmation dans l'email");
        }
        return matcher.group(1).replace("&amp;", "&");
    }
}
