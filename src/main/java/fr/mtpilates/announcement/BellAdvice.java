package fr.mtpilates.announcement;

import fr.mtpilates.announcement.AnnouncementViews.Bell;
import fr.mtpilates.auth.CurrentUserService;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Données de la cloche du header (annonces non lues et dernières annonces), sur toutes les pages connectées. */
@ControllerAdvice
public class BellAdvice {

    private static final Bell EMPTY = new Bell(0, List.of());

    private final AnnouncementService announcements;
    private final CurrentUserService currentUsers;

    public BellAdvice(final AnnouncementService announcements, final CurrentUserService currentUsers) {
        this.announcements = announcements;
        this.currentUsers = currentUsers;
    }

    @ModelAttribute("bell")
    public Bell bell() {
        return currentUsers.current().map(announcements::bell).orElse(EMPTY);
    }
}
