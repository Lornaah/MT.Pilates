package fr.mtpilates.tracking;

import static fr.mtpilates.shared.web.FormValues.date;
import static fr.mtpilates.shared.web.FormValues.integer;
import static fr.mtpilates.shared.web.FormValues.text;

import fr.mtpilates.auth.CurrentUser;
import fr.mtpilates.member.MemberGuard;
import fr.mtpilates.shared.ui.FrenchDates;
import fr.mtpilates.tracking.TrackingViews.ReviewView;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bilans hebdomadaires : un par semaine, ré-enregistrer la même semaine la remplace (upsert de l'original). */
@Service
public class WeeklyReviewService {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 10;
    private static final int DEFAULT_SCORE = 7;

    private final WeeklyReviewRepository reviews;
    private final MemberGuard guard;
    private final Clock clock;

    public WeeklyReviewService(final WeeklyReviewRepository reviews, final MemberGuard guard, final Clock clock) {
        this.reviews = reviews;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ReviewView> reviews(final CurrentUser user, final UUID memberId) {
        guard.requireManage(user, memberId);
        return reviews.findByUserIdOrderByWeekStartDateDesc(memberId).stream()
                .map(r -> new ReviewView(r.getId(), FrenchDates.shortDate(r.getWeekStartDate()), r.getMentalWellbeing(),
                        r.getFreeNote()))
                .toList();
    }

    /** Lundi de la semaine en cours, valeur par défaut du formulaire. */
    public LocalDate currentWeekStart() {
        return LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    @Transactional
    public void record(final CurrentUser user, final String weekStart, final String score, final String note) {
        final LocalDate week = date(weekStart).orElseGet(this::currentWeekStart);
        final int wellbeing = Math.clamp(integer(score).orElse(DEFAULT_SCORE), MIN_SCORE, MAX_SCORE);
        final WeeklyReview review = reviews.findByUserIdAndWeekStartDate(user.id(), week)
                .orElseGet(() -> new WeeklyReview(user.id(), week));
        review.record(wellbeing, text(note).orElse(null));
        reviews.save(review);
    }

    @Transactional
    public void delete(final CurrentUser user, final UUID reviewId) {
        final WeeklyReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new AccessDeniedException("Bilan introuvable"));
        guard.requireManage(user, review.getUserId());
        reviews.delete(review);
    }
}
