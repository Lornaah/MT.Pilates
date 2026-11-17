package fr.mtpilates.tracking;

import static fr.mtpilates.shared.web.FormValues.date;
import static fr.mtpilates.shared.web.FormValues.decimal;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.tracking.TrackingViews.EntryView;
import fr.mtpilates.tracking.TrackingViews.MeasureValue;
import fr.mtpilates.tracking.TrackingViews.Trend;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mensurations : saisies et supprimées par le membre, consultées par le staff. */
@Service
public class TrackingService {

    /** L'original n'affichait que les 30 dernières entrées, et 500 pour le graphique. */
    private static final Limit LATEST = Limit.of(30);
    private static final Limit CHART = Limit.of(500);

    private final TrackingEntryRepository entries;
    private final MemberGuard guard;
    private final Clock clock;

    public TrackingService(final TrackingEntryRepository entries, final MemberGuard guard, final Clock clock) {
        this.entries = entries;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EntryView> entries(final CurrentUser user, final UUID memberId) {
        guard.requireReadTracking(user, memberId);
        final List<TrackingEntry> latest = entries.findByUserIdOrderByDateDesc(memberId, LATEST);
        final List<EntryView> views = new ArrayList<>();
        for (int i = 0; i < latest.size(); i++) {
            final Optional<TrackingEntry> previous = i + 1 < latest.size() ? Optional.of(latest.get(i + 1)) : Optional.empty();
            views.add(toView(latest.get(i), previous));
        }
        return views;
    }

    /** Données brutes pour le graphique d'évolution (ordre chronologique). */
    @Transactional(readOnly = true)
    public List<TrackingEntry> history(final CurrentUser user, final UUID memberId) {
        guard.requireReadTracking(user, memberId);
        return entries.findByUserIdOrderByDateDesc(memberId, CHART).reversed();
    }

    @Transactional
    public void add(final CurrentUser user, final TrackingEntryForm form) {
        final Map<Measure, BigDecimal> values = new EnumMap<>(Measure.class);
        putIfPresent(values, Measure.WEIGHT, form.weightKg());
        putIfPresent(values, Measure.BODY_FAT, form.bodyFatPct());
        putIfPresent(values, Measure.WAIST, form.waistCm());
        putIfPresent(values, Measure.HIP, form.hipCm());
        putIfPresent(values, Measure.CHEST, form.chestCm());
        putIfPresent(values, Measure.ARM, form.armCm());
        putIfPresent(values, Measure.THIGH, form.thighCm());
        final LocalDate day = date(form.date()).orElseGet(() -> LocalDate.now(clock));
        entries.save(new TrackingEntry(user.id(), day, values, text(form.notes()).orElse(null)));
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID entryId) {
        final TrackingEntry entry = entries.findById(entryId).filter(e -> e.getUserId().equals(user.id()))
                .orElseThrow(() -> new AccessDeniedException("Entrée introuvable"));
        entries.delete(entry);
    }

    private static void putIfPresent(final Map<Measure, BigDecimal> values, final Measure measure, final String raw) {
        decimal(raw).ifPresent(value -> values.put(measure, value));
    }

    private static EntryView toView(final TrackingEntry entry, final Optional<TrackingEntry> previous) {
        final Map<Measure, BigDecimal> before = previous.map(TrackingEntry::values).orElse(Map.of());
        final List<MeasureValue> values = entry.values().entrySet().stream()
                .map(e -> new MeasureValue(e.getKey().label(), plain(e.getValue()), e.getKey().unit(),
                        trend(e.getValue(), Optional.ofNullable(before.get(e.getKey())))))
                .toList();
        return new EntryView(entry.getId(), FrenchDates.full(entry.getDate()), values, entry.getNotes());
    }

    private static Trend trend(final BigDecimal current, final Optional<BigDecimal> previous) {
        return previous.map(p -> switch (Integer.signum(current.compareTo(p))) {
            case -1 -> Trend.DOWN;
            case 1 -> Trend.UP;
            default -> Trend.SAME;
        }).orElse(Trend.NONE);
    }

    static String plain(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
