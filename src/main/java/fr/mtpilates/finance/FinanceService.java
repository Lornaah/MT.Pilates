package fr.mtpilates.finance;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import fr.mtpilates.finance.FinanceViews.EditValues;
import fr.mtpilates.finance.FinanceViews.Page;
import fr.mtpilates.finance.FinanceViews.Row;
import fr.mtpilates.member.MemberAccessPolicy;
import fr.mtpilates.prospect.Prospect;
import fr.mtpilates.prospect.ProspectRepository;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suivi financier (FinanceTracking) : abonnés visibles, source synchronisée depuis le lead converti, KPIs,
 * graphiques et export, calculés sur la liste filtrée comme dans l'original.
 */
@Service
public class FinanceService {

    /** Sources proposées d'office (SOURCES de l'original), avant celles trouvées sur les fiches. */
    static final List<String> DEFAULT_SOURCES = List.of("Instagram", "TikTok", "Booking", "Site web", "Bouche-à-oreille",
            "Parrainage", "Autre");
    static final Map<String, String[]> STATUS_META = statusMeta();
    private static final int MONTHS = 12;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH);

    private final ProfileRepository profiles;
    private final ProspectRepository prospects;
    private final MemberAccessPolicy policy;
    private final Clock clock;

    public FinanceService(final ProfileRepository profiles, final ProspectRepository prospects,
                          final MemberAccessPolicy policy, final Clock clock) {
        this.profiles = profiles;
        this.prospects = prospects;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page page(final CurrentUser user, final FinanceFilters filters) {
        final Map<UUID, Prospect> byMember = prospectsByMember(user);
        final List<Profile> visible = visible(user);
        final List<String> sources = sources(visible, byMember);
        final List<Profile> filtered = visible.stream().filter(m -> matches(m, filters, byMember)).toList();
        final BigDecimal contracted = sum(filtered, Profile::getContractAmount);
        final BigDecimal paid = sum(filtered, Profile::getPaidAmount);
        final BigDecimal remaining = contracted.subtract(paid);
        return new Page(sorted(filtered, filters, byMember).stream().map(m -> row(m, byMember)).toList(),
                (int) filtered.stream().filter(m -> "active".equals(m.getStatus().orElse(null))).count(), eur(contracted),
                eur(paid), eur(remaining), remaining.signum() > 0, charts(filtered, byMember), sources);
    }

    /** Export CSV de la liste filtrée (séparateur « ; », BOM ajouté par le contrôleur). */
    @Transactional(readOnly = true)
    public String csv(final CurrentUser user, final FinanceFilters filters) {
        final Map<UUID, Prospect> byMember = prospectsByMember(user);
        final List<String> lines = new ArrayList<>();
        lines.add(String.join(";", "Nom", "Prénom", "Email", "Téléphone", "Statut", "Source", "CA contracté", "CA collecté",
                "Reste", "Début abonnement", "Fin abonnement", "Notes"));
        visible(user).stream().filter(m -> matches(m, filters, byMember)).forEach(m -> {
            final BigDecimal c = amount(m.getContractAmount());
            final BigDecimal p = amount(m.getPaidAmount());
            lines.add(String.join(";", List.of(m.getLastName().orElse(""), m.getFirstName().orElse(""), m.getEmail(),
                            m.getPhone().orElse(""), statusLabel(m), source(m, byMember).orElse(""), fixed(c), fixed(p),
                            fixed(c.subtract(p)), m.getSubscriptionStartDate().map(LocalDate::toString).orElse(""),
                            m.getSubscriptionEndDate().map(LocalDate::toString).orElse(""),
                            m.getBillingNotes().orElse("").replace("\n", " "))
                    .stream().map(FinanceService::csvEscape).toList()));
        });
        return String.join("\n", lines);
    }

    @Transactional(readOnly = true)
    public EditValues editValues(final CurrentUser user, final UUID memberId) {
        final Profile m = updatable(user, memberId);
        final Map<UUID, Prospect> byMember = prospectsByMember(user);
        return new EditValues(m.getId(), "Suivi financier — " + name(m),
                Optional.ofNullable(byMember.get(m.getId())).flatMap(p -> Optional.ofNullable(p.content().source()))
                        .flatMap(FinanceService::prettySource),
                m.getBillingSource().orElse(""), plain(m.getContractAmount()), plain(m.getPaidAmount()),
                String.valueOf(m.getPaymentInstallments()), m.getBillingNotes().orElse(""), sources(visible(user), byMember));
    }

    @Transactional
    public void update(final CurrentUser user, final UUID memberId, final BillingForm form) {
        final Profile m = updatable(user, memberId);
        m.updateBilling(Optional.ofNullable(form.source()).filter(s -> !s.isEmpty()).orElse(null), number(form.contracted()),
                number(form.paid()), Math.max(1, number(form.installments()).intValue()),
                Optional.ofNullable(form.notes()).filter(s -> !s.isEmpty()).orElse(null));
        profiles.save(m);
    }

    @Transactional(readOnly = true)
    public String memberName(final CurrentUser user, final UUID memberId) {
        return name(updatable(user, memberId));
    }

    // ------------------------------------------------------------------ lignes et filtres

    private Row row(final Profile m, final Map<UUID, Prospect> byMember) {
        final BigDecimal c = amount(m.getContractAmount());
        final BigDecimal p = amount(m.getPaidAmount());
        final BigDecimal remaining = c.subtract(p);
        final String[] meta = STATUS_META.getOrDefault(m.getStatus().orElse("active"), STATUS_META.get("active"));
        final double ratio = c.signum() > 0 ? Math.min(100, p.doubleValue() / c.doubleValue() * 100) : 0;
        return new Row(m.getId(), name(m), m.getEmail(), m.getStatus().orElse("active"), meta[0], meta[1],
                source(m, byMember), byMember.containsKey(m.getId()), eur(c), eur(p), eur(remaining), remaining.signum() > 0,
                c.signum() > 0, ratio, m.getPhone().filter(s -> !s.isEmpty()), sanitizePhone(m.getPhone().orElse("")),
                reminder(m, remaining));
    }

    private static boolean matches(final Profile m, final FinanceFilters f, final Map<UUID, Prospect> byMember) {
        if (!"all".equals(f.statusOrAll()) && !f.statusOrAll().equals(m.getStatus().orElse(null))) {
            return false;
        }
        if (!"all".equals(f.sourceOrAll()) && !f.sourceOrAll().equals(source(m, byMember).orElse(""))) {
            return false;
        }
        if (f.remainingOnly() && amount(m.getContractAmount()).subtract(amount(m.getPaidAmount())).signum() <= 0) {
            return false;
        }
        final String haystack = (m.getFirstName().orElse("") + " " + m.getLastName().orElse("") + " " + m.getEmail())
                .toLowerCase(Locale.ROOT);
        return f.query().isEmpty() || haystack.contains(f.query());
    }

    /** Tri de l'original : comparaison `<` sur chaînes ou nombres, tri stable. */
    private static List<Profile> sorted(final List<Profile> rows, final FinanceFilters f, final Map<UUID, Prospect> byMember) {
        final Comparator<Profile> comparator = switch (f.sortKey()) {
            case "name" -> Comparator.comparing(m -> (m.getLastName().orElse("") + " " + m.getFirstName().orElse(""))
                    .toLowerCase(Locale.ROOT));
            case "status" -> Comparator.comparing(m -> m.getStatus().orElse(""));
            case "source" -> Comparator.comparing(m -> source(m, byMember).orElse(""));
            case "contracted" -> Comparator.comparing(m -> amount(m.getContractAmount()));
            case "paid" -> Comparator.comparing(m -> amount(m.getPaidAmount()));
            default -> Comparator.comparing(m -> amount(m.getContractAmount()).subtract(amount(m.getPaidAmount())));
        };
        return rows.stream().sorted(f.ascending() ? comparator : comparator.reversed()).toList();
    }

    // ------------------------------------------------------------------ graphiques (buildChartData)

    private Map<String, Object> charts(final List<Profile> members, final Map<UUID, Prospect> byMember) {
        final YearMonth current = YearMonth.now(clock);
        final Map<YearMonth, BigDecimal[]> monthly = new LinkedHashMap<>();
        for (int i = MONTHS - 1; i >= 0; i--) {
            monthly.put(current.minusMonths(i), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        final Map<String, BigDecimal> bySource = new LinkedHashMap<>();
        final Map<String, Integer> byStatus = new LinkedHashMap<>();
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;
        for (final Profile m : members) {
            final BigDecimal c = amount(m.getContractAmount());
            final BigDecimal p = amount(m.getPaidAmount());
            paid = paid.add(p);
            remaining = remaining.add(c.subtract(p).max(BigDecimal.ZERO));
            m.getSubscriptionStartDate().map(YearMonth::from).map(monthly::get).ifPresent(cell -> {
                cell[0] = cell[0].add(c);
                cell[1] = cell[1].add(p);
            });
            bySource.merge(source(m, byMember).orElse("Non renseignée"), c, BigDecimal::add);
            byStatus.merge(m.getStatus().filter(s -> !s.isEmpty()).orElse("active"), 1, Integer::sum);
        }
        final Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("monthly", monthly.entrySet().stream().map(e -> Map.of("month", e.getKey().atDay(1).format(MONTH_LABEL),
                "contracted", round(e.getValue()[0]), "paid", round(e.getValue()[1]))).toList());
        charts.put("bySource", bySource.entrySet().stream().map(e -> Map.<String, Object>of("name", e.getKey(), "value", round(e.getValue())))
                .filter(d -> ((Long) d.get("value")) > 0)
                .sorted(Comparator.comparing((Map<String, Object> d) -> (Long) d.get("value")).reversed()).toList());
        charts.put("byStatus", byStatus.entrySet().stream().map(e -> Map.<String, Object>of("name",
                STATUS_META.containsKey(e.getKey()) ? STATUS_META.get(e.getKey())[0] : e.getKey(), "value", e.getValue())).toList());
        charts.put("collection", List.of(Map.of("name", "Encaissé", "value", round(paid)),
                Map.of("name", "Restant", "value", round(remaining))));
        return charts;
    }

    // ------------------------------------------------------------------ données visibles

    /** Profils lisibles (policy de profiles) : tout pour le management, sinon soi et ses coachés. */
    private List<Profile> visible(final CurrentUser user) {
        return DataAccessRoles.isManagement(user.realRoles()) ? profiles.findAllByOrderByCreatedAtDesc()
                : profiles.findOwnOrCoachedOrderByCreatedAtDesc(user.id());
    }

    /** Leads convertis, lisibles par le management seulement : sans eux, pas de source synchronisée. */
    private Map<UUID, Prospect> prospectsByMember(final CurrentUser user) {
        if (!DataAccessRoles.isManagement(user.realRoles())) {
            return Map.of();
        }
        return prospects.findAllByOrderByCreatedAtDesc().stream().filter(p -> p.getConvertedMemberId().isPresent())
                .collect(Collectors.toMap(p -> p.getConvertedMemberId().get(), Function.identity(), (a, b) -> b));
    }

    private Profile updatable(final CurrentUser user, final UUID memberId) {
        return profiles.findById(memberId).filter(p -> policy.canUpdate(user, p.getId(), p.getCoachId()))
                .orElseThrow(() -> new AccessDeniedException("Fiche non modifiable"));
    }

    private static List<String> sources(final List<Profile> members, final Map<UUID, Prospect> byMember) {
        final Set<String> set = new LinkedHashSet<>(DEFAULT_SOURCES);
        members.forEach(m -> source(m, byMember).ifPresent(set::add));
        return List.copyOf(set);
    }

    /** Source du lead d'origine si le membre vient d'une conversion, sinon la source saisie à la main. */
    static Optional<String> source(final Profile m, final Map<UUID, Prospect> byMember) {
        return Optional.ofNullable(byMember.get(m.getId())).flatMap(p -> Optional.ofNullable(p.content().source()))
                .flatMap(FinanceService::prettySource).or(() -> m.getBillingSource().flatMap(FinanceService::prettySource));
    }

    /** `_` → espace et majuscule en début de mot, au sens ASCII de `\b\w` en JavaScript. */
    static Optional<String> prettySource(final String raw) {
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        final String spaced = raw.replace('_', ' ');
        final StringBuilder out = new StringBuilder(spaced.length());
        for (int i = 0; i < spaced.length(); i++) {
            final char ch = spaced.charAt(i);
            final boolean startOfWord = isWordChar(ch) && (i == 0 || !isWordChar(spaced.charAt(i - 1)));
            out.append(startOfWord ? Character.toUpperCase(ch) : ch);
        }
        return Optional.of(out.toString());
    }

    private static boolean isWordChar(final char ch) {
        return ch < 128 && (Character.isLetterOrDigit(ch) || ch == '_');
    }

    // ------------------------------------------------------------------ rappel WhatsApp

    /** sanitizePhone : 00 et + retirés, 0 initial remplacé par l'indicatif français. */
    static String sanitizePhone(final String raw) {
        String s = raw.replaceAll("[\\s.\\-()]", "");
        if (s.startsWith("+")) {
            s = s.substring(1);
        } else if (s.startsWith("00")) {
            s = s.substring(2);
        } else if (s.startsWith("0")) {
            s = "33" + s.substring(1);
        }
        return s.replaceAll("\\D", "");
    }

    private static String reminder(final Profile m, final BigDecimal remaining) {
        return "Coucou " + m.getFirstName().map(String::trim).orElse("") + " 🌸\n\nPetit rappel concernant ton suivi : il reste "
                + eur(remaining.max(BigDecimal.ZERO)) + " à régler sur ton accompagnement.\n\nN'hésite pas à me dire si tu as "
                + "la moindre question, je suis là pour toi !\n\nBelle journée 💛";
    }

    // ------------------------------------------------------------------ formats

    /** Intl.NumberFormat("fr-FR", EUR) : « 1 234,50 € ». */
    static String eur(final BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.FRANCE).format(value);
    }

    private static String statusLabel(final Profile m) {
        final String status = m.getStatus().orElse("active");
        return STATUS_META.containsKey(status) ? STATUS_META.get(status)[0] : status;
    }

    private static String name(final Profile m) {
        return m.getFirstName().orElse("") + " " + m.getLastName().orElse("");
    }

    private static BigDecimal amount(final BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal sum(final List<Profile> members, final Function<Profile, BigDecimal> field) {
        return members.stream().map(field).map(FinanceService::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static long round(final BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static String fixed(final BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String plain(final BigDecimal value) {
        return amount(value).stripTrailingZeros().toPlainString();
    }

    private static BigDecimal number(final String value) {
        return fr.mtpilates.shared.web.FormValues.decimal(value).orElse(BigDecimal.ZERO);
    }

    private static String csvEscape(final String value) {
        return value.matches("(?s).*[\",;\\n].*") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }

    private static Map<String, String[]> statusMeta() {
        final Map<String, String[]> meta = new LinkedHashMap<>();
        meta.put("active", new String[] {"Actif", "bg-sage/15 text-sage border-sage/30"});
        meta.put("paused", new String[] {"En pause", "bg-amber-500/15 text-amber-600 border-amber-500/30"});
        meta.put("finished", new String[] {"Terminé", "bg-primary/15 text-primary border-primary/30"});
        meta.put("archived", new String[] {"Archivé", "bg-muted text-muted-foreground border-border"});
        return java.util.Collections.unmodifiableMap(meta);
    }
}
