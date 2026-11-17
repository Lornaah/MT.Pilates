package fr.mtpilates.prospect;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.auth.EmailAlreadyRegisteredException;
import fr.mtpilates.prospect.ProspectViews.CallRow;
import fr.mtpilates.prospect.ProspectViews.Calls;
import fr.mtpilates.prospect.ProspectViews.Column;
import fr.mtpilates.prospect.ProspectViews.FormValues;
import fr.mtpilates.prospect.ProspectViews.Row;
import fr.mtpilates.prospect.ProspectViews.Stats;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.shared.ui.WhatsAppLinks;
import fr.mtpilates.shared.web.ClockConfig;
import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.user.UserAccount;
import fr.mtpilates.user.UserAccountRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline commercial (Leads, Closing). Policy de l'original : seul le management (owner, setter) lit et écrit
 * les leads ; les autres rôles de la rubrique « Ventes » voient des listes vides.
 */
@Service
public class ProspectService {

    public static final List<String> STATUSES = List.of("new", "to_call", "trial", "converted", "lost");
    public static final List<String> SOURCES = List.of("instagram", "tiktok", "website", "word_of_mouth", "referral", "other");
    public static final List<String> COLUMNS = List.of("new", "to_call", "trial", "converted");
    private static final int MIN_PASSWORD = 8;
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final ProspectRepository prospects;
    private final UserAccountRepository accounts;
    private final ProfileRepository profiles;
    private final UserRoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public ProspectService(final ProspectRepository prospects, final UserAccountRepository accounts,
                           final ProfileRepository profiles, final UserRoleRepository roles,
                           final PasswordEncoder passwordEncoder, final Clock clock) {
        this.prospects = prospects;
        this.accounts = accounts;
        this.profiles = profiles;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Row> list(final CurrentUser user) {
        if (!management(user)) {
            return List.of();
        }
        return prospects.findAllByOrderByCreatedAtDesc().stream().map(this::row).toList();
    }

    public Stats stats(final List<Row> rows) {
        return new Stats(rows.size(), count(rows, "to_call"), (int) rows.stream().filter(this::upcoming).count(),
                count(rows, "converted"));
    }

    /** Colonnes du pipeline ; « Perdus » n'apparaît que s'il en existe. */
    public List<Column> columns(final List<Row> rows) {
        final List<String> statuses = rows.stream().anyMatch(r -> "lost".equals(r.status()))
                ? STATUSES : COLUMNS;
        return statuses.stream().map(s -> new Column(s, rows.stream().filter(r -> s.equals(r.status())).toList())).toList();
    }

    /** Sources présentes parmi les leads, dans l'ordre de première apparition (filtre du tableau). */
    public List<String> sources(final List<Row> rows) {
        return rows.stream().map(Row::source).filter(s -> !s.isEmpty()).distinct().toList();
    }

    @Transactional(readOnly = true)
    public Calls calls(final CurrentUser user) {
        final List<Prospect> all = management(user) ? prospects.findAllByOrderByCreatedAtDesc() : List.of();
        final Instant now = clock.instant();
        final List<CallRow> upcoming = all.stream()
                .filter(p -> p.content().appointmentAt() != null && !p.content().appointmentAt().isBefore(now))
                .sorted(Comparator.comparing(p -> p.content().appointmentAt()))
                .map(p -> new CallRow(row(p), Optional.of(dayTime(p.content().appointmentAt())))).toList();
        final List<CallRow> toCall = all.stream().filter(p -> "to_call".equals(p.content().status()))
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.content().nextFollowupAt()).orElse(p.getCreatedAt())))
                .map(p -> new CallRow(row(p), Optional.ofNullable(p.content().nextFollowupAt()).map(ProspectService::dayTime)))
                .toList();
        final List<CallRow> trial = all.stream().filter(p -> "trial".equals(p.content().status()))
                .map(p -> new CallRow(row(p), Optional.empty())).toList();
        return new Calls(upcoming, toCall, trial);
    }

    @Transactional(readOnly = true)
    public FormValues formValues(final CurrentUser user, final Optional<UUID> id) {
        requireManagement(user);
        return id.flatMap(prospects::findById).map(p -> values(p.getId(), p.content()))
                .orElseGet(() -> new FormValues(Optional.empty(), "", "", "", "", "", "instagram", "new", "", "", "", "", ""));
    }

    @Transactional(readOnly = true)
    public Optional<Row> find(final CurrentUser user, final UUID id) {
        requireManagement(user);
        return prospects.findById(id).map(this::row);
    }

    @Transactional
    public void save(final CurrentUser user, final Optional<UUID> id, final ProspectForm form) {
        requireManagement(user);
        final String firstName = text(form.firstName()).orElse("");
        if (firstName.isEmpty()) {
            throw new IllegalArgumentException("Prénom requis");
        }
        final ProspectContent content = new ProspectContent(firstName, text(form.lastName()).orElse(null),
                text(form.email()).orElse(null), text(form.phone()).orElse(null), integer(form.age()).orElse(null),
                SOURCES.contains(String.valueOf(form.source())) ? form.source() : "instagram",
                STATUSES.contains(String.valueOf(form.status())) ? form.status() : "new", text(form.tag()).orElse(null),
                text(form.notes()).orElse(null), text(form.objectives()).orElse(null), instant(form.nextFollowupAt()),
                instant(form.appointmentAt()));
        final Prospect prospect = id.flatMap(prospects::findById).orElseGet(() -> new Prospect(user.id(), clock.instant()));
        prospect.update(content, clock.instant());
        prospects.save(prospect);
    }

    @Transactional
    public void changeStatus(final CurrentUser user, final UUID id, final String status) {
        requireManagement(user);
        if (!STATUSES.contains(String.valueOf(status))) {
            throw new IllegalArgumentException("Statut inconnu");
        }
        prospects.findById(id).ifPresent(p -> p.changeStatus(status, clock.instant()));
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID id) {
        requireManagement(user);
        prospects.deleteById(id);
    }

    /**
     * Conversion en membre (fonction convert-prospect de l'original), ici en une seule transaction : compte, fiche
     * (coach, téléphone, statut actif), rôle membre et lead marqué « converti ».
     */
    @Transactional
    public UUID convert(final CurrentUser user, final UUID id, final ConvertForm form) {
        requireManagement(user);
        final Prospect prospect = prospects.findById(id).orElseThrow(() -> new IllegalArgumentException("Prospect not found"));
        final ProspectContent c = prospect.content();
        final String email = text(c.email()).map(e -> e.toLowerCase(Locale.ROOT)).orElseThrow(
                () -> new IllegalArgumentException("Le prospect doit avoir un email pour être converti"));
        final String password = Optional.ofNullable(form.password()).orElse("");
        if (password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("Mot de passe : 8 caractères minimum");
        }
        if (accounts.existsByEmail(email)) {
            throw new IllegalArgumentException(new EmailAlreadyRegisteredException().getMessage());
        }
        final UUID memberId = UUID.randomUUID();
        accounts.save(new UserAccount(memberId, email, passwordEncoder.encode(password)));
        final Profile profile = new Profile(memberId, email);
        profile.updateIdentity(c.firstName(), c.lastName(), c.phone(), null);
        profile.updateMembership("active", null, null, coach(form.coachId()).orElse(null));
        profiles.save(profile);
        roles.save(new UserRole(memberId, AppRole.MEMBER));
        prospect.convertedTo(memberId, clock.instant());
        return memberId;
    }

    private Row row(final Prospect p) {
        final ProspectContent c = p.content();
        final String name = c.firstName() + " " + Optional.ofNullable(c.lastName()).orElse("");
        final String search = (c.firstName() + " " + orEmpty(c.lastName()) + " " + orEmpty(c.email()) + " " + orEmpty(c.phone()))
                .toLowerCase(Locale.ROOT);
        return new Row(p.getId(), name, c.firstName(), p.getConvertedMemberId(), text(c.email()), text(c.phone()),
                text(c.tag()), text(c.notes()), text(c.objectives()), Optional.ofNullable(c.age()).filter(a -> a != 0),
                Optional.ofNullable(c.source()).orElse(""), Optional.ofNullable(c.status()).orElse("new"),
                FrenchDates.medium(local(p.getCreatedAt()).toLocalDate()), Optional.ofNullable(c.appointmentAt()),
                Optional.ofNullable(c.appointmentAt()).map(i -> FrenchDates.shortDateTime(local(i))),
                Optional.ofNullable(c.nextFollowupAt()).map(i -> FrenchDates.shortDateTime(local(i))),
                p.getConvertedMemberId().isEmpty() && text(c.email()).isPresent(), search,
                text(c.phone()).map(phone -> WhatsAppLinks.link(phone, "Bonjour " + c.firstName()
                        + ", je reviens vers vous suite à notre échange. Avez-vous pu réfléchir à la mise en place de votre coaching ?")),
                text(c.phone()).map(phone -> WhatsAppLinks.link(phone, "Bonjour " + c.firstName()
                        + ", je reviens vers vous suite à notre échange. Êtes-vous toujours intéressé(e) pour démarrer votre coaching ?")));
    }

    private boolean upcoming(final Row row) {
        return row.appointmentAt().filter(a -> !a.isBefore(clock.instant())).isPresent();
    }

    private static FormValues values(final UUID id, final ProspectContent c) {
        return new FormValues(Optional.of(id), c.firstName(), orEmpty(c.lastName()), orEmpty(c.email()), orEmpty(c.phone()),
                c.age() == null ? "" : c.age().toString(), Optional.ofNullable(c.source()).orElse("instagram"),
                Optional.ofNullable(c.status()).orElse("new"), orEmpty(c.tag()), orEmpty(c.notes()), orEmpty(c.objectives()),
                input(c.nextFollowupAt()), input(c.appointmentAt()));
    }

    private Optional<UUID> coach(final String coachId) {
        try {
            return text(coachId).filter(v -> !"none".equals(v)).map(UUID::fromString);
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static int count(final List<Row> rows, final String status) {
        return (int) rows.stream().filter(r -> status.equals(r.status())).count();
    }

    private static boolean management(final CurrentUser user) {
        return DataAccessRoles.isManagement(user.realRoles());
    }

    private static void requireManagement(final CurrentUser user) {
        if (!management(user)) {
            throw new AccessDeniedException("Réservé au management");
        }
    }

    private static LocalDateTime local(final Instant instant) {
        return LocalDateTime.ofInstant(instant, ClockConfig.STUDIO_ZONE);
    }

    private static String dayTime(final Instant instant) {
        return FrenchDates.dayDateTime(local(instant));
    }

    private static String input(final Instant instant) {
        return instant == null ? "" : local(instant).format(INPUT);
    }

    private static Instant instant(final String value) {
        try {
            return text(value).map(v -> LocalDateTime.parse(v.trim()).atZone(ClockConfig.STUDIO_ZONE).toInstant()).orElse(null);
        } catch (final DateTimeParseException e) {
            return null;
        }
    }

    private static Optional<String> text(final String value) {
        return fr.mtpilates.shared.web.FormValues.text(value).map(String::trim);
    }

    private static Optional<Integer> integer(final String value) {
        return fr.mtpilates.shared.web.FormValues.integer(value);
    }

    private static String orEmpty(final String value) {
        return value == null ? "" : value;
    }
}
