package fr.mtpilates.profile;

import static fr.mtpilates.shared.web.FormValues.date;
import static fr.mtpilates.shared.web.FormValues.decimal;
import static fr.mtpilates.shared.web.FormValues.integer;
import static fr.mtpilates.shared.web.FormValues.list;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import fr.mtpilates.user.MemberSummary;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fiche personnelle (Ma fiche, onboarding, avatar). Règle d'origine : chacun modifie sa propre fiche. */
@Service
public class ProfileService {

    static final String AVATAR_BUCKET = "avatars";

    private final ProfileRepository profiles;
    private final StorageService storage;
    private final Clock clock;

    public ProfileService(final ProfileRepository profiles, final StorageService storage, final Clock clock) {
        this.profiles = profiles;
        this.storage = storage;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileView own(final CurrentUser user) {
        return ProfileView.of(load(user.id()));
    }

    /** Vrai pour un membre (non staff) qui n'a pas encore terminé l'onboarding (OnboardingGate de l'original). */
    @Transactional(readOnly = true)
    public boolean needsOnboarding(final CurrentUser user) {
        return !user.capacities().isStaff()
                && profiles.findById(user.id()).map(p -> p.getOnboardingCompletedAt().isEmpty()).orElse(false);
    }

    @Transactional
    public void updateOwn(final CurrentUser user, final ProfileForm form) {
        final Profile profile = load(user.id());
        applyIdentityAndSport(profile, form);
        profile.updateSummary(summaryOf(form));
    }

    @Transactional
    public void completeOnboarding(final CurrentUser user, final ProfileForm form) {
        final Profile profile = load(user.id());
        applyIdentityAndSport(profile, form);
        profile.updateSummary(summaryOf(form));
        profile.updateEquipmentAndAvailability(list(form.equipment()), availability(list(form.slots())));
        profile.replaceDietaryPreferences(dietaryPreferences(profile.getDietaryPreferences(), form));
        profile.completeOnboarding(clock.instant());
    }

    @Transactional
    public void changeAvatar(final CurrentUser user, final String originalFilename, final String contentType,
                             final InputStream content) throws IOException {
        final String extension = extensionOf(originalFilename);
        final StorageKey key = new StorageKey(AVATAR_BUCKET, user.id() + "/avatar." + extension);
        storage.store(key, content, contentType);
        // Cache-bust, comme l'original, pour que la nouvelle photo s'affiche immédiatement.
        load(user.id()).changeAvatar("/files/" + AVATAR_BUCKET + "/" + key.path() + "?t=" + clock.millis());
    }

    private void applyIdentityAndSport(final Profile profile, final ProfileForm form) {
        profile.updateIdentity(text(form.firstName()).orElse(""), text(form.lastName()).orElse(""),
                text(form.phone()).orElse(""), date(form.dateOfBirth()).orElse(null));
        final int weekly = integer(form.weeklyTargetSessions()).filter(v -> v != 0).orElse(2);
        profile.updateSportProfile(text(form.goals()).orElse(""), positive(form.heightCm()), positive(form.weightKg()),
                weekly, text(form.bio()).orElse(""));
    }

    /** L'original n'enregistrait la taille et le poids que s'ils étaient renseignés (0 compris comme vide). */
    private static BigDecimal positive(final String value) {
        return decimal(value).filter(v -> v.signum() != 0).orElse(null);
    }

    private static MemberSummary summaryOf(final ProfileForm form) {
        return new MemberSummary(list(form.objectives()), list(form.allergies()), list(form.pathologies()),
                text(form.workSedentarity()).orElse(null), text(form.bodyType()).orElse(null),
                text(form.sportHistory()).orElse(null), text(form.coachingMotivation()).orElse(null),
                text(form.externalSummary()).orElse(null));
    }

    private static Map<String, List<String>> availability(final List<String> slots) {
        final Map<String, List<String>> byDay = new LinkedHashMap<>();
        for (final String slot : slots) {
            final String[] parts = slot.split(":", 2);
            if (parts.length == 2) {
                byDay.computeIfAbsent(parts[0], day -> new ArrayList<>()).add(parts[1]);
            }
        }
        return byDay;
    }

    /** Les préférences existantes sont conservées et complétées, comme setDiet({ ...dietary, ...patch }). */
    private static Map<String, Object> dietaryPreferences(final Map<String, Object> existing, final ProfileForm form) {
        final Map<String, Object> merged = new LinkedHashMap<>(existing);
        merged.put("diet_type", text(form.dietType()).orElse("Omnivore"));
        merged.put("dislikes", list(form.dislikes()));
        merged.put("likes", list(form.likes()));
        merged.put("meals_per_day", integer(form.mealsPerDay()).filter(v -> v != 0).orElse(3));
        merged.put("weekly_budget", decimal(form.weeklyBudget()).orElse(null));
        text(form.cookingTime()).ifPresent(time -> merged.put("cooking_time", time));
        return merged;
    }

    private Profile load(final UUID id) {
        return profiles.findById(id).orElseThrow(() -> new IllegalStateException("Fiche introuvable : " + id));
    }

    private static String extensionOf(final String filename) {
        final String name = Optional.ofNullable(filename).orElse("");
        final int dot = name.lastIndexOf('.');
        final String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return ext.matches("[a-z0-9]{1,8}") ? ext : "png";
    }

}
