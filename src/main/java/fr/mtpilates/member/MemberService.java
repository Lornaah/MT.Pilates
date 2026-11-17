package fr.mtpilates.member;

import static fr.mtpilates.shared.web.FormValues.date;
import static fr.mtpilates.shared.web.FormValues.list;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.auth.EmailAlreadyRegisteredException;
import fr.mtpilates.auth.EmailRules;
import fr.mtpilates.auth.PasswordPolicy;
import fr.mtpilates.profile.ProfileView;
import fr.mtpilates.user.AppRole;
import fr.mtpilates.user.MemberSummary;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileOptions;
import fr.mtpilates.user.ProfileRepository;
import fr.mtpilates.user.UserAccount;
import fr.mtpilates.user.UserAccountRepository;
import fr.mtpilates.user.UserRole;
import fr.mtpilates.user.UserRoleRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Membres vus par le staff : listes « Mes coachés » / « Tous les membres », création, modification, suppression. */
@Service
public class MemberService {

    private static final int NAME_MAX_LENGTH = 60;
    private static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProfileRepository profiles;
    private final UserAccountRepository accounts;
    private final UserRoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final MemberAccessPolicy policy;

    public MemberService(final ProfileRepository profiles, final UserAccountRepository accounts,
                         final UserRoleRepository roles, final PasswordEncoder passwordEncoder,
                         final MemberAccessPolicy policy) {
        this.profiles = profiles;
        this.accounts = accounts;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.policy = policy;
    }

    /** « Mes coachés » : les membres assignés au coach connecté. */
    @Transactional(readOnly = true)
    public List<MemberRow> coachedBy(final CurrentUser user, final MemberFilters filters) {
        return rows(user, profiles.findByCoachIdOrderByCreatedAtDesc(user.id()),
                new MemberFilters(filters.search(), MemberFilters.ALL, filters.status()));
    }

    /** « Tous les membres » : toutes les fiches visibles (toutes pour le management, comme le SELECT * de l'original). */
    @Transactional(readOnly = true)
    public List<MemberRow> visibleTo(final CurrentUser user, final MemberFilters filters) {
        final List<Profile> visible = DataAccessRoles.isManagement(user.realRoles())
                ? profiles.findAllByOrderByCreatedAtDesc()
                : profiles.findOwnOrCoachedOrderByCreatedAtDesc(user.id());
        return rows(user, visible, filters);
    }

    /** Utilisateurs ayant le rôle coach (listCoaches de l'original). */
    @Transactional(readOnly = true)
    public List<CoachOption> coaches() {
        final List<UUID> ids = roles.findByRole(AppRole.COACH).stream().map(UserRole::getUserId).toList();
        return ids.isEmpty() ? List.of() : profiles.findByIdInOrderByCreatedAtAsc(ids).stream()
                .map(p -> new CoachOption(p.getId(), fullName(p), p.getEmail(), CoachBadges.classFor(Optional.of(p.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberEditView editable(final CurrentUser user, final UUID memberId) {
        final Profile member = updatable(user, memberId);
        return new MemberEditView(member.getId(),
                ("Modifier " + member.getFirstName().orElse("") + " " + member.getLastName().orElse("")).trim(),
                member.getStatus().orElse(MemberStatus.ACTIVE.code()),
                member.getSubscriptionStartDate().map(LocalDate::toString).orElse(""),
                member.getSubscriptionEndDate().map(LocalDate::toString).orElse(""),
                member.getPhone().orElse(""),
                member.getCoachId().map(UUID::toString).orElse(""));
    }

    @Transactional
    public void update(final CurrentUser user, final UUID memberId, final EditMemberForm form) {
        final Profile member = updatable(user, memberId);
        final String status = MemberStatus.fromCode(form.status()).map(MemberStatus::code)
                .orElse(member.getStatus().orElse(MemberStatus.ACTIVE.code()));
        member.updateMembership(status, date(form.subscriptionStartDate()).orElse(null),
                date(form.subscriptionEndDate()).orElse(null), coachId(form.coachId()).orElse(null));
        member.changePhone(text(form.phone()).orElse(null));
    }

    /**
     * Crée le compte, la fiche et le rôle membre. Pas d'email de confirmation : c'est le management qui
     * communique les identifiants, comme dans l'original.
     */
    @Transactional
    public UUID create(final CurrentUser user, final CreateMemberForm form) {
        if (!policy.canCreate(user)) {
            throw new AccessDeniedException("Création réservée au management");
        }
        final String email = trimmed(form.email()).toLowerCase(Locale.ROOT);
        final String firstName = trimmed(form.firstName());
        final String lastName = trimmed(form.lastName());
        final List<String> errors = creationErrors(email, Optional.ofNullable(form.password()).orElse(""), firstName, lastName);
        if (!errors.isEmpty()) {
            throw new MemberCreationException(String.join(" • ", errors));
        }
        if (accounts.existsByEmail(email)) {
            throw new MemberCreationException(new EmailAlreadyRegisteredException().getMessage());
        }
        final UUID id = UUID.randomUUID();
        accounts.save(new UserAccount(id, email, passwordEncoder.encode(form.password())));
        final Profile profile = new Profile(id, email);
        profile.updateIdentity(firstName, lastName, null, null);
        profiles.save(profile);
        roles.save(new UserRole(id, AppRole.MEMBER));
        return id;
    }

    @Transactional(readOnly = true)
    public MemberDetailView detail(final CurrentUser user, final UUID memberId) {
        final Profile member = profiles.findById(memberId)
                .filter(p -> policy.canView(user, p.getId(), p.getCoachId()))
                .orElseThrow(() -> new AccessDeniedException("Fiche non consultable"));
        final String name = fullName(member).trim();
        return new MemberDetailView(member.getId(), name.isEmpty() ? member.getEmail() : name, member.getEmail(),
                member.getAvatarUrl().filter(url -> !url.isEmpty()).orElse(null),
                initialOf(member).toUpperCase(Locale.ROOT),
                member.getStatus().orElse(MemberStatus.ACTIVE.code()),
                member.getCoachId().map(UUID::toString).orElse(""),
                member.getSubscriptionStartDate().map(LocalDate::toString).orElse(""),
                member.getSubscriptionEndDate().map(LocalDate::toString).orElse(""),
                ProfileOptions.sedentarityLabel(member.getWorkSedentarity().orElse(null)),
                ProfileOptions.bodyTypeLabel(member.getBodyType().orElse(null)),
                ProfileView.of(member));
    }

    /** Champs administratifs pris en compte seulement pour un manager : l'original ne les envoyait pas sinon. */
    @Transactional
    public void saveDetail(final CurrentUser user, final UUID memberId, final MemberDetailForm form) {
        final Profile member = updatable(user, memberId);
        member.updateCoachNotes(text(form.goals()).orElse(null), text(form.coachNotes()).orElse(null));
        member.changeWeeklyTarget(member.getWeeklyTargetSessions().filter(v -> v != 0).orElse(2));
        member.updateSummary(new MemberSummary(list(form.objectives()), list(form.allergies()), list(form.pathologies()),
                text(form.workSedentarity()).orElse(null), text(form.bodyType()).orElse(null),
                text(form.sportHistory()).orElse(null), text(form.coachingMotivation()).orElse(null),
                text(form.externalSummary()).orElse(null)));
        if (user.capacities().isManager()) {
            final String status = MemberStatus.fromCode(form.status()).map(MemberStatus::code)
                    .orElse(member.getStatus().orElse(MemberStatus.ACTIVE.code()));
            member.updateMembership(status, date(form.subscriptionStartDate()).orElse(null),
                    date(form.subscriptionEndDate()).orElse(null), coachId(form.coachId()).orElse(null));
        }
    }

    /** Supprime le compte entier (fiche, rôles et données liées en cascade) plutôt que la seule fiche (Q2). */
    @Transactional
    public void delete(final CurrentUser user, final UUID memberId) {
        final boolean isOwner = roles.existsByUserIdAndRole(memberId, AppRole.OWNER);
        if (!policy.canDelete(user, memberId, isOwner)) {
            throw new AccessDeniedException("Suppression refusée");
        }
        accounts.deleteById(memberId);
    }

    /** Ordre et messages du signupSchema de l'original (email, mot de passe, prénom, nom). */
    private static List<String> creationErrors(final String email, final String password, final String firstName,
                                               final String lastName) {
        final List<String> errors = new ArrayList<>();
        EmailRules.firstError(email).ifPresent(errors::add);
        errors.addAll(PasswordPolicy.violations(password));
        if (firstName.isEmpty()) {
            errors.add("Prénom requis");
        } else if (firstName.length() > NAME_MAX_LENGTH) {
            errors.add("Prénom trop long");
        }
        if (lastName.length() > NAME_MAX_LENGTH) {
            errors.add("Nom trop long");
        }
        return errors;
    }

    private Profile updatable(final CurrentUser user, final UUID memberId) {
        final Profile member = profiles.findById(memberId).orElseThrow(() -> new AccessDeniedException("Fiche introuvable"));
        if (!policy.canUpdate(user, member.getId(), member.getCoachId())) {
            throw new AccessDeniedException("Fiche non modifiable");
        }
        return member;
    }

    private List<MemberRow> rows(final CurrentUser user, final List<Profile> source, final MemberFilters filters) {
        final Map<UUID, CoachOption> coachesById = coaches().stream()
                .collect(Collectors.toMap(CoachOption::id, Function.identity()));
        final List<UUID> ownerIds = roles.findByRole(AppRole.OWNER).stream().map(UserRole::getUserId).toList();
        final String query = filters.search().trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(p -> matchesSearch(p, query))
                .filter(p -> MemberFilters.ALL.equals(filters.coach())
                        || p.getCoachId().map(UUID::toString).filter(filters.coach()::equals).isPresent())
                .filter(p -> MemberFilters.ALL.equals(filters.status())
                        || p.getStatus().filter(filters.status()::equals).isPresent())
                .map(p -> toRow(p, coachesById, policy.canDelete(user, p.getId(), ownerIds.contains(p.getId()))))
                .toList();
    }

    private static boolean matchesSearch(final Profile profile, final String query) {
        return query.isEmpty()
                || profile.getEmail().toLowerCase(Locale.ROOT).contains(query)
                || profile.getFirstName().map(s -> s.toLowerCase(Locale.ROOT).contains(query)).orElse(false)
                || profile.getLastName().map(s -> s.toLowerCase(Locale.ROOT).contains(query)).orElse(false);
    }

    private static MemberRow toRow(final Profile p, final Map<UUID, CoachOption> coachesById, final boolean deletable) {
        final Optional<CoachOption> coach = p.getCoachId().map(coachesById::get);
        return new MemberRow(p.getId(), p.getFirstName().orElse("") + " " + p.getLastName().orElse(""), p.getEmail(),
                p.getAvatarUrl().filter(url -> !url.isEmpty()), initialOf(p), p.getPhone().filter(s -> !s.isEmpty()),
                coach.map(CoachOption::name), CoachBadges.classFor(p.getCoachId()),
                p.getStatus().orElse(MemberStatus.ACTIVE.code()), subscription(p), deletable);
    }

    /** « Abo. jusqu'au … » prioritaire sur « Depuis le … », dates au format toLocaleDateString("fr-FR"). */
    private static Optional<String> subscription(final Profile p) {
        return p.getSubscriptionEndDate().map(end -> "Abo. jusqu'au " + frenchDate(end))
                .or(() -> p.getSubscriptionStartDate().map(start -> "Depuis le " + frenchDate(start)));
    }

    private static String frenchDate(final LocalDate date) {
        return date.format(FRENCH_DATE);
    }

    private static String fullName(final Profile p) {
        return p.getFirstName().orElse("") + " " + p.getLastName().orElse("");
    }

    private static String initialOf(final Profile p) {
        final String source = p.getFirstName().filter(s -> !s.isEmpty()).orElse(p.getEmail());
        return source.isEmpty() ? "?" : source.substring(0, 1);
    }

    private static String trimmed(final String value) {
        return Optional.ofNullable(value).map(String::trim).orElse("");
    }

    private static Optional<UUID> coachId(final String value) {
        return text(value).filter(v -> !"none".equals(v)).flatMap(MemberService::uuid);
    }

    private static Optional<UUID> uuid(final String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
