package fr.mtpilates.shared.security;

/** Découpage des routes par garde, repris de App.tsx (StaffGate, OwnerGate). */
final class AppPaths {

    static final String[] AUTHENTICATED = {"/app", "/app/**", "/onboarding"};

    static final String[] STAFF = {
        "/app/calendar/**", "/app/coach/**", "/app/prospects/**", "/app/calls/**",
        "/app/my-booking-link/**", "/app/admin/members/**", "/app/admin/prospects/**", "/app/admin/finance/**",
    };

    /** /app/money y figure : la sidebar la réservait à l'owner mais la route était ouverte à tout le staff (faille corrigée, Q2). */
    static final String[] OWNER = {"/app/admin/team/**", "/app/owner/**", "/app/money/**"};

    private AppPaths() {
    }
}
