package fr.mtpilates.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Fiche d'un utilisateur (table profiles) : identité, profil sportif et nutrition, suivi coach, facturation. */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String bio;

    private String goals;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "weekly_target_sessions")
    private Integer weeklyTargetSessions;

    @Column(name = "coach_notes")
    private String coachNotes;

    private String status;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "coach_id")
    private UUID coachId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> allergies = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> pathologies = new ArrayList<>();

    @Column(name = "work_sedentarity")
    private String workSedentarity;

    @Column(name = "sport_history")
    private String sportHistory;

    @Column(name = "body_type")
    private String bodyType;

    @Column(name = "coaching_motivation")
    private String coachingMotivation;

    @Column(name = "external_summary")
    private String externalSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_preferences", nullable = false)
    private Map<String, Object> dietaryPreferences = new LinkedHashMap<>();

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> objectives = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "available_equipment", columnDefinition = "text[]", nullable = false)
    private List<String> availableEquipment = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, List<String>> availability = new LinkedHashMap<>();

    @Column(name = "billing_source")
    private String billingSource;

    @Column(name = "contract_amount", nullable = false)
    private BigDecimal contractAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "payment_installments", nullable = false)
    private int paymentInstallments = 1;

    @Column(name = "billing_notes")
    private String billingNotes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Profile() {
    }

    public Profile(final UUID id, final String email) {
        this.id = id;
        this.email = email;
        this.status = "active";
        this.weeklyTargetSessions = 2;
    }

    // ---------------------------------------------------------------- identité

    public void updateIdentity(final String newFirstName, final String newLastName, final String newPhone,
                               final LocalDate newDateOfBirth) {
        this.firstName = newFirstName;
        this.lastName = newLastName;
        this.phone = newPhone;
        this.dateOfBirth = newDateOfBirth;
    }

    public void changeAvatar(final String url) {
        this.avatarUrl = url;
    }

    // ------------------------------------------------------------- suivi sportif

    public void updateSportProfile(final String newGoals, final BigDecimal newHeight, final BigDecimal newWeight,
                                   final int newWeeklyTarget, final String newBio) {
        this.goals = newGoals;
        this.heightCm = newHeight;
        this.weightKg = newWeight;
        this.weeklyTargetSessions = newWeeklyTarget;
        this.bio = newBio;
    }

    public void updateSummary(final MemberSummary summary) {
        this.objectives = new ArrayList<>(summary.objectives());
        this.allergies = new ArrayList<>(summary.allergies());
        this.pathologies = new ArrayList<>(summary.pathologies());
        this.workSedentarity = summary.workSedentarity();
        this.bodyType = summary.bodyType();
        this.sportHistory = summary.sportHistory();
        this.coachingMotivation = summary.coachingMotivation();
        this.externalSummary = summary.externalSummary();
    }

    public void updateEquipmentAndAvailability(final List<String> equipment, final Map<String, List<String>> slots) {
        this.availableEquipment = new ArrayList<>(equipment);
        this.availability = new LinkedHashMap<>(slots);
    }

    public void replaceDietaryPreferences(final Map<String, Object> preferences) {
        this.dietaryPreferences = new LinkedHashMap<>(preferences);
    }

    public void completeOnboarding(final Instant now) {
        this.termsAcceptedAt = now;
        this.onboardingCompletedAt = now;
    }

    // --------------------------------------------------------- suivi par le coach

    public void updateCoachNotes(final String newGoals, final String notes) {
        this.goals = newGoals;
        this.coachNotes = notes;
    }

    public void updateMembership(final String newStatus, final LocalDate start, final LocalDate end, final UUID newCoachId) {
        this.status = newStatus;
        this.subscriptionStartDate = start;
        this.subscriptionEndDate = end;
        this.coachId = newCoachId;
    }

    public void changePhone(final String newPhone) {
        this.phone = newPhone;
    }

    public void changeWeeklyTarget(final int target) {
        this.weeklyTargetSessions = target;
    }

    // ------------------------------------------------------------- facturation

    public void updateBilling(final String source, final BigDecimal contract, final BigDecimal paid,
                              final int installments, final String notes) {
        this.billingSource = source;
        this.contractAmount = contract;
        this.paidAmount = paid;
        this.paymentInstallments = installments;
        this.billingNotes = notes;
    }

    // ----------------------------------------------------------------- lecture

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Optional<String> getFirstName() {
        return Optional.ofNullable(firstName);
    }

    public Optional<String> getLastName() {
        return Optional.ofNullable(lastName);
    }

    public Optional<String> getPhone() {
        return Optional.ofNullable(phone);
    }

    public Optional<LocalDate> getDateOfBirth() {
        return Optional.ofNullable(dateOfBirth);
    }

    public Optional<String> getAvatarUrl() {
        return Optional.ofNullable(avatarUrl);
    }

    public Optional<String> getBio() {
        return Optional.ofNullable(bio);
    }

    public Optional<String> getGoals() {
        return Optional.ofNullable(goals);
    }

    public Optional<BigDecimal> getHeightCm() {
        return Optional.ofNullable(heightCm);
    }

    public Optional<BigDecimal> getWeightKg() {
        return Optional.ofNullable(weightKg);
    }

    public Optional<Integer> getWeeklyTargetSessions() {
        return Optional.ofNullable(weeklyTargetSessions);
    }

    public Optional<String> getCoachNotes() {
        return Optional.ofNullable(coachNotes);
    }

    public Optional<String> getStatus() {
        return Optional.ofNullable(status);
    }

    public Optional<LocalDate> getSubscriptionStartDate() {
        return Optional.ofNullable(subscriptionStartDate);
    }

    public Optional<LocalDate> getSubscriptionEndDate() {
        return Optional.ofNullable(subscriptionEndDate);
    }

    public Optional<UUID> getCoachId() {
        return Optional.ofNullable(coachId);
    }

    public List<String> getAllergies() {
        return List.copyOf(allergies);
    }

    public List<String> getPathologies() {
        return List.copyOf(pathologies);
    }

    public Optional<String> getWorkSedentarity() {
        return Optional.ofNullable(workSedentarity);
    }

    public Optional<String> getSportHistory() {
        return Optional.ofNullable(sportHistory);
    }

    public Optional<String> getBodyType() {
        return Optional.ofNullable(bodyType);
    }

    public Optional<String> getCoachingMotivation() {
        return Optional.ofNullable(coachingMotivation);
    }

    public Optional<String> getExternalSummary() {
        return Optional.ofNullable(externalSummary);
    }

    public Map<String, Object> getDietaryPreferences() {
        return dietaryPreferences == null ? Map.of() : Map.copyOf(dietaryPreferences);
    }

    public Optional<Instant> getOnboardingCompletedAt() {
        return Optional.ofNullable(onboardingCompletedAt);
    }

    public List<String> getObjectives() {
        return List.copyOf(objectives);
    }

    public List<String> getAvailableEquipment() {
        return List.copyOf(availableEquipment);
    }

    public Map<String, List<String>> getAvailability() {
        return availability == null ? Map.of() : Map.copyOf(availability);
    }

    public Optional<String> getBillingSource() {
        return Optional.ofNullable(billingSource);
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public int getPaymentInstallments() {
        return paymentInstallments;
    }

    public Optional<String> getBillingNotes() {
        return Optional.ofNullable(billingNotes);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** « Prénom Nom », ou l'email si les deux sont vides (règle d'affichage récurrente de l'original). */
    public String displayName() {
        final String name = (getFirstName().orElse("") + " " + getLastName().orElse("")).trim();
        return name.isEmpty() ? email : name;
    }
}
