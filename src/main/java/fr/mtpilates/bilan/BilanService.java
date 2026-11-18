package fr.mtpilates.bilan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.auth.DataAccessRoles;
import org.springframework.security.access.AccessDeniedException;
import fr.mtpilates.bilan.BilanViews.Editor;
import fr.mtpilates.bilan.BilanViews.MetricOption;
import fr.mtpilates.bilan.BilanViews.Photo;
import fr.mtpilates.bilan.BilanViews.Summary;
import fr.mtpilates.member.MemberBuckets;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.shared.storage.StorageKey;
import fr.mtpilates.shared.storage.StorageService;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.shared.web.FormValues;
import fr.mtpilates.tracking.MemberPhoto;
import fr.mtpilates.tracking.MemberPhotoRepository;
import fr.mtpilates.tracking.PhotoSide;
import fr.mtpilates.tracking.TrackingEntry;
import fr.mtpilates.tracking.TrackingEntryRepository;
import fr.mtpilates.user.Profile;
import fr.mtpilates.user.ProfileRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bilans d'un membre : brouillon pré-rempli, réglages du coach, aperçu, envoi (PDF figé, email, espace membre).
 * Règles de member_reports : le staff qui gère le membre fait tout ; le membre lit ses bilans envoyés.
 */
@Service
public class BilanService {

    static final String BUCKET = "member-reports";
    private static final String PHOTO_PREFIX = "/files/" + MemberBuckets.PHOTOS + "/";
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final MemberReportRepository reports;
    private final TrackingEntryRepository tracking;
    private final MemberPhotoRepository photos;
    private final ProfileRepository profiles;
    private final BilanTemplateService templates;
    private final BilanPdfRenderer renderer;
    private final BilanMailer mailer;
    private final StorageService storage;
    private final MemberGuard guard;
    private final ObjectMapper json;
    private final Clock clock;

    public BilanService(final MemberReportRepository reports, final TrackingEntryRepository tracking,
                        final MemberPhotoRepository photos, final ProfileRepository profiles,
                        final BilanTemplateService templates, final BilanPdfRenderer renderer, final BilanMailer mailer,
                        final StorageService storage, final MemberGuard guard, final ObjectMapper json, final Clock clock) {
        this.reports = reports;
        this.tracking = tracking;
        this.photos = photos;
        this.profiles = profiles;
        this.templates = templates;
        this.renderer = renderer;
        this.mailer = mailer;
        this.storage = storage;
        this.guard = guard;
        this.json = json;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ liste et création

    @Transactional(readOnly = true)
    public List<Summary> forMember(final CurrentUser user, final UUID memberId) {
        requireStaffManage(user, memberId);
        return reports.findByUserIdOrderByCreatedAtDesc(memberId).stream().map(this::summary).toList();
    }

    /** Bilans envoyés au membre connecté (« Mes bilans »). */
    @Transactional(readOnly = true)
    public List<Summary> mine(final CurrentUser user) {
        return reports.findByUserIdAndStatusOrderBySentAtDesc(user.id(), "sent").stream().map(this::summary).toList();
    }

    /**
     * Brouillon prêt à relire : dernier mois, objectif du bilan précédent, mensurations disponibles cochées,
     * photos avant / après de chaque vue (plus ancienne et plus récente), textes du modèle courant.
     */
    @Transactional
    public UUID createDraft(final CurrentUser user, final UUID memberId) {
        requireStaffManage(user, memberId);
        final LocalDate end = LocalDate.now(clock);
        final LocalDate start = end.minusMonths(1);
        final Optional<BigDecimal> target = reports.findFirstByUserIdAndTargetWeightKgNotNullOrderByCreatedAtDesc(memberId)
                .flatMap(MemberReport::getTargetWeightKg);
        final BilanCalculator.Snapshot snapshot = snapshot(memberId, start, end, Optional.empty(), target.map(BigDecimal::doubleValue));
        final List<String> measures = snapshot.metrics().stream().filter(BilanCalculator.Metric::renderable)
                .map(m -> m.metric().key()).toList();
        final BilanContent content = new BilanContent(Map.of(), measures, defaultViews(memberId), templates.current());
        final MemberReport report = new MemberReport(memberId, user.id());
        report.updateSettings(start, end, target.orElse(null), null, toMap(content));
        return reports.save(report).getId();
    }

    // ------------------------------------------------------------------ éditeur

    @Transactional(readOnly = true)
    public Editor editor(final CurrentUser user, final UUID reportId) {
        final MemberReport report = managed(user, reportId);
        final BilanCalculator.Snapshot snapshot = snapshot(report);
        final List<MetricOption> metrics = snapshot.metrics().stream().map(m -> new MetricOption(m.metric().key(),
                m.metric().label(), m.renderable(), m.delta().map(d -> BilanVariables.delta(d, m.metric().unit())).orElse("—"))).toList();
        return new Editor(report.getId(), report.getUserId(), memberName(report.getUserId()), report.isSent(),
                report.getPeriodStart().toString(), report.getPeriodEnd().toString(),
                report.getTargetWeightKg().map(BilanService::plain).orElse(""),
                report.getStartWeightOverrideKg().map(BilanService::plain).orElse(""), write(report.getContent()),
                photos.findByUserIdOrderByTakenAtDesc(report.getUserId()).stream().map(BilanService::photo).toList(), metrics,
                BilanVariables.computedDurations(snapshot, firstFollowUp(report.getUserId())));
    }

    /**
     * Enregistrement des réglages. Pour un bilan déjà envoyé, la membre garde la version envoyée tant que le coach
     * n'a pas publié la mise à jour. Renvoie vrai si le bilan a déjà été envoyé.
     */
    @Transactional
    public boolean save(final CurrentUser user, final UUID reportId, final BilanSettingsForm form) {
        final MemberReport report = managed(user, reportId);
        final LocalDate start = date(form.periodStart()).orElse(report.getPeriodStart());
        final LocalDate end = date(form.periodEnd()).orElse(report.getPeriodEnd());
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("La date de fin doit suivre la date de début");
        }
        final BilanContent content = parse(form.content());
        report.updateSettings(start, end, FormValues.decimal(form.targetWeight()).orElse(null),
                FormValues.decimal(form.startWeight()).orElse(null), toMap(content));
        return report.isSent();
    }

    /** Aperçu des réglages enregistrés, y compris les modifications pas encore publiées d'un bilan envoyé. */
    @Transactional(readOnly = true)
    public byte[] preview(final CurrentUser user, final UUID reportId) {
        return render(managed(user, reportId));
    }

    /** Issue d'un envoi ou d'une mise à jour. */
    public enum SendResult { MAILED, MAIL_FAILED, UPDATED }

    /**
     * Envoi : PDF stocké, instantané des chiffres conservé, email à la membre avec le PDF en pièce jointe.
     * Pour un bilan déjà envoyé, c'est une mise à jour : le PDF de l'espace membre est remplacé et l'email ne repart
     * que si le coach le demande. Si l'email échoue, le bilan reste disponible dans l'espace de la membre.
     */
    @Transactional
    public SendResult send(final CurrentUser user, final UUID reportId, final String appLink, final boolean notify) {
        final MemberReport report = managed(user, reportId);
        final boolean update = report.isSent();
        final byte[] pdf = render(report);
        final String path = report.getUserId() + "/" + report.getId() + ".pdf";
        storage.store(new StorageKey(BUCKET, path), new ByteArrayInputStream(pdf), "application/pdf");
        report.markSent(snapshotJson(snapshot(report)), path, clock.instant());
        if (update && !notify) {
            return SendResult.UPDATED;
        }
        final Profile member = profiles.findById(report.getUserId()).orElseThrow();
        return mailer.send(member.getEmail(), member.getFirstName().orElse(""), filename(report), pdf, appLink)
                ? SendResult.MAILED : SendResult.MAIL_FAILED;
    }

    @Transactional
    public UUID delete(final CurrentUser user, final UUID reportId) {
        final MemberReport report = managed(user, reportId);
        reports.delete(report);
        report.getPdfPath().ifPresent(p -> storage.delete(new StorageKey(BUCKET, p)));
        return report.getUserId();
    }

    /** Téléchargement : le membre pour ses bilans envoyés, le staff qui gère le membre pour tous. */
    @Transactional(readOnly = true)
    public Optional<Download> download(final CurrentUser user, final UUID reportId) {
        return reports.findById(reportId)
                .filter(r -> r.isSent() && r.getUserId().equals(user.id()) || canStaffManage(user, r.getUserId()))
                .map(r -> new Download(r.isSent() ? stored(r).orElseGet(() -> render(r)) : render(r), filename(r)));
    }

    // canManage couvre aussi le membre lui-même : les brouillons et l'éditeur restent réservés au staff.
    private boolean canStaffManage(final CurrentUser user, final UUID memberId) {
        return DataAccessRoles.isStaff(user.realRoles()) && guard.canManage(user, memberId);
    }

    private void requireStaffManage(final CurrentUser user, final UUID memberId) {
        if (!canStaffManage(user, memberId)) {
            throw new AccessDeniedException("Membre hors de portée");
        }
    }

    public record Download(byte[] pdf, String filename) {
    }

    // ------------------------------------------------------------------ calcul et rendu

    private byte[] render(final MemberReport report) {
        final BilanCalculator.Snapshot snapshot = snapshot(report);
        final BilanContent content = content(report);
        final Map<String, String> durations = new LinkedHashMap<>(BilanVariables.computedDurations(snapshot,
                firstFollowUp(report.getUserId())));
        content.durationsOrEmpty().forEach((k, v) -> {
            if (durations.containsKey(k) && v != null && !v.isBlank()) {
                durations.put(k, v.trim());
            }
        });
        final String firstName = profiles.findById(report.getUserId()).flatMap(Profile::getFirstName).orElse("");
        final Map<String, String> variables = BilanVariables.of(firstName, snapshot, durations);
        final Map<String, String> texts = new LinkedHashMap<>(templates.current());
        texts.putAll(content.textsOrEmpty());
        final BilanContent withTexts = new BilanContent(content.durations(), content.measures(), content.views(), texts);
        final BilanDocument document = BilanDocumentFactory.build(withTexts, snapshot, variables, id -> photoBytes(report.getUserId(), id));
        return renderer.pdf(document, "Bilan de " + firstName);
    }

    private BilanCalculator.Snapshot snapshot(final MemberReport report) {
        return snapshot(report.getUserId(), report.getPeriodStart(), report.getPeriodEnd(),
                report.getStartWeightOverrideKg().map(BigDecimal::doubleValue), report.getTargetWeightKg().map(BigDecimal::doubleValue));
    }

    private BilanCalculator.Snapshot snapshot(final UUID memberId, final LocalDate start, final LocalDate end,
                                              final Optional<Double> override, final Optional<Double> target) {
        final List<BilanCalculator.Entry> entries = tracking.findByUserIdOrderByDateAsc(memberId).stream()
                .map(BilanService::entry).toList();
        return BilanCalculator.compute(entries, start, end, override, target);
    }

    private static BilanCalculator.Entry entry(final TrackingEntry e) {
        final Map<BilanMetric, BigDecimal> values = new EnumMap<>(BilanMetric.class);
        for (final BilanMetric metric : BilanMetric.values()) {
            final BigDecimal value = e.values().get(metric.measure());
            if (value != null) {
                values.put(metric, value);
            }
        }
        return new BilanCalculator.Entry(e.getDate(), values);
    }

    private Optional<LocalDate> firstFollowUp(final UUID memberId) {
        return tracking.findByUserIdOrderByDateAsc(memberId).stream().findFirst().map(TrackingEntry::getDate);
    }

    private Map<String, BilanContent.View> defaultViews(final UUID memberId) {
        final List<MemberPhoto> all = photos.findByUserIdOrderByTakenAtDesc(memberId);
        final Map<String, BilanContent.View> views = new LinkedHashMap<>();
        for (final PhotoSide side : PhotoSide.values()) {
            final List<MemberPhoto> sidePhotos = all.stream().filter(p -> p.getSide() == side)
                    .sorted(Comparator.comparing(MemberPhoto::getTakenAt)).toList();
            if (sidePhotos.size() >= 2) {
                views.put(side.code(), new BilanContent.View(true, sidePhotos.get(0).getId().toString(),
                        sidePhotos.get(sidePhotos.size() - 1).getId().toString(), List.of(), List.of()));
            }
        }
        return views;
    }

    private Optional<byte[]> photoBytes(final UUID memberId, final String photoId) {
        try {
            return photos.findById(UUID.fromString(photoId)).filter(p -> p.getUserId().equals(memberId))
                    .filter(p -> p.getPhotoUrl().startsWith(PHOTO_PREFIX))
                    .flatMap(p -> storage.load(new StorageKey(MemberBuckets.PHOTOS, p.getPhotoUrl().substring(PHOTO_PREFIX.length()))))
                    .flatMap(BilanService::read);
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Optional<byte[]> stored(final MemberReport report) {
        return report.getPdfPath().flatMap(p -> storage.load(new StorageKey(BUCKET, p))).flatMap(BilanService::read);
    }

    private static Optional<byte[]> read(final Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return Optional.of(in.readAllBytes());
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ vues

    private Summary summary(final MemberReport r) {
        final BilanCalculator.Snapshot s = snapshot(r);
        return new Summary(r.getId(), r.getUserId(), FrenchDates.medium(r.getPeriodStart()) + " → " + FrenchDates.medium(r.getPeriodEnd()),
                r.isSent(), FrenchDates.medium(LocalDate.ofInstant(r.getCreatedAt(), clock.getZone())),
                r.getSentAt().map(i -> FrenchDates.medium(LocalDate.ofInstant(i, clock.getZone()))),
                s.metric(BilanMetric.WEIGHT).flatMap(BilanCalculator.Metric::delta).map(d -> BilanVariables.delta(d, "kg")));
    }

    private static Photo photo(final MemberPhoto p) {
        return new Photo(p.getId().toString(), p.getPhotoUrl(), p.getSide().code(), FrenchDates.medium(p.getTakenAt()));
    }

    private String memberName(final UUID memberId) {
        return profiles.findById(memberId).map(Profile::displayName).orElse("");
    }

    private static String filename(final MemberReport r) {
        return "bilan-" + r.getPeriodEnd() + ".pdf";
    }

    private MemberReport managed(final CurrentUser user, final UUID reportId) {
        final MemberReport report = reports.findById(reportId).orElseThrow(() -> new AccessDeniedException("Bilan introuvable"));
        requireStaffManage(user, report.getUserId());
        return report;
    }

    // ------------------------------------------------------------------ JSON

    private BilanContent content(final MemberReport report) {
        return json.convertValue(report.getContent(), BilanContent.class);
    }

    private BilanContent parse(final String value) {
        try {
            return json.readValue(Optional.ofNullable(value).filter(v -> !v.isBlank()).orElse("{}"), BilanContent.class);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Réglages du bilan invalides");
        }
    }

    private Map<String, Object> toMap(final BilanContent content) {
        return json.convertValue(content, MAP);
    }

    private Map<String, Object> snapshotJson(final BilanCalculator.Snapshot s) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("periodStart", s.periodStart().toString());
        map.put("periodEnd", s.periodEnd().toString());
        s.startWeight().ifPresent(v -> map.put("startWeightKg", v));
        s.currentWeight().ifPresent(v -> map.put("currentWeightKg", v));
        s.targetWeight().ifPresent(v -> map.put("targetWeightKg", v));
        s.progressRatio().ifPresent(v -> map.put("progressRatio", v));
        map.put("metrics", s.metrics().stream().map(m -> {
            final Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("key", m.metric().key());
            m.start().ifPresent(v -> metric.put("start", v));
            m.end().ifPresent(v -> metric.put("end", v));
            m.delta().ifPresent(v -> metric.put("delta", v));
            return metric;
        }).toList());
        return map;
    }

    private String write(final Map<String, Object> value) {
        try {
            return json.writeValueAsString(value);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Contenu du bilan non sérialisable", e);
        }
    }

    private static Optional<LocalDate> date(final String value) {
        try {
            return FormValues.text(value).map(LocalDate::parse);
        } catch (final DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String plain(final BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
