# PLAN.md — Réécriture M.T.Pilates en Spring Boot

Chaque module est livrable seul : code + tests + `./mvnw clean verify` vert + comparaison visuelle des écrans
du module avec l'original + un commit. On ne passe au module suivant que sur demande.
Les références « legacy » sont relatives à `../legacy/mt.pilate.app-main/src/`.

Décisions : `INVENTORY.md` §9. Reste R3 : identifiants des comptes de test, à transmettre de façon sûre en M0.

## Critères communs de « terminé »
- Tests : unitaires (services, `AccessPolicy`, calculs) + intégration Testcontainers (repositories, controllers MockMvc
  avec utilisateurs de chaque rôle, y compris les refus 403/redirections).
- Chaque règle RLS du module (`INVENTORY.md` §4) a au moins un test positif et un test négatif.
- Rendu page complète **et** fragment HTMX testés pour chaque controller interactif ; erreurs de validation re-rendues en fragment.
- Captures Playwright (desktop 1440 px + mobile 390 px) de chaque écran, ancien vs nouveau, jointes au compte rendu du module.

---

## M0 — Socle
**But** : un projet qui démarre, une base migrée, un layout vide au rendu identique.
- Projet Maven `fr.mtpilates`, Java 21, Spring Boot 3.x, wrapper, profils `dev` / `test`, `docker-compose.yml` (PostgreSQL).
- **Flyway `V1__baseline.sql`** : schéma consolidé (état final des 38 migrations) — enums, tables, contraintes CHECK/UNIQUE,
  index, FK, triggers `updated_at` conservés ; enums en texte + CHECK ; table `users` applicative à la place de `auth.users` ;
  vue `v_announcements_for_user` remplacée par une requête de repository ; seed `app_config` (libellés, `program_objectives`, `onboarding_video_url`).
- Build front : `frontend-maven-plugin` (Node + npm), Tailwind v4 + Basecoat + typography + tw-animate-css, HTMX, Alpine, Chart.js, Lucide ; sortie dans `static/`.
- **Design tokens** : variables HSL dans `themes/default.css`, mappées dans `@theme` (couleurs étendues, radius, ombres, dégradés, `.grain`,
  animations, polices, `.skip-link`) ; couleurs en dur de l'original converties en variables ; `themes/dark.css` porté, non activé ;
  test de fumée : une page `/dev/ui?theme=dark` (dev uniquement) prouve qu'un thème s'applique sans changer de template.
- Démon Docker requis pour Testcontainers (à démarrer dans l'environnement).
- Accès à l'ancienne app pour la recette visuelle : lancement local du front legacy (Vite) branché sur l'instance Supabase existante avec le compte de test (R3).
- **Bibliothèque de fragments UI** (`templates/fragments/ui/`) : button (10 variantes × 4 tailles), badge (10 variantes), card,
  input/label/textarea/select/checkbox/switch, tabs, dialog, confirm-dialog, form-dialog, popover, tooltip, toast, table, icon (Lucide),
  page-header, stat-card, empty-state, loading-spinner, user-avatar, tag-input, date-picker / date-time-picker (Alpine).
  Une page interne `/dev/ui` (profil `dev` uniquement) affiche tous les composants pour la recette visuelle.
- Infrastructure web : détection `HX-Request`, toasts via `HX-Trigger`, CSRF pour HTMX, gestion d'erreurs (404 identique à l'original), locale `fr`.
- `StorageService` (interface) + `LocalStorageService` (répertoire configurable) + controller de téléchargement générique (autorisation déléguée).
- **Landing `/`** (statique : hero, sections, ScrollReveal en Alpine/IntersectionObserver, assets) — sert de premier écran de recette visuelle.
- Manifest PWA, favicon, icônes.
- Tests : démarrage du contexte, migration Flyway sur Testcontainers, rendu de la landing et de `/dev/ui`.
- Legacy : `index.css`, `tailwind.config.ts`, `components/ui/*`, `components/common/*`, `pages/Index.tsx`, `pages/NotFound.tsx`, `index.html`, `public/*`.

## M1 — Authentification et rôles
- Entités `User`, `Profile`, `UserRole` ; enum `AppRole`.
- Spring Security : form login sur `/auth` (onglet Connexion), session, déconnexion → `/` ; « Se souvenir de moi » = pré-remplissage email
  (localStorage via Alpine, comme l'original) ; traductions des messages d'erreur.
- Inscription (onglet Créer un compte) : **email + mot de passe** (`@StrongPassword` 24–72 + 4 classes, jauge de force en Alpine)
  → table `pending_registrations` (email, hash BCrypt, jeton haché, expiration) → email de vérification → au clic, création
  user + profil + rôle `member` (ou `owner` si l'email figure dans `APP_BOOTSTRAP_OWNER_EMAILS`) → connexion → onboarding.
  « Renvoyer l'email de confirmation » conservé ; purge des inscriptions expirées.
  Envoi via `spring-boot-starter-mail` (capture locale en dev, SMTP de prod selon R2) ; templates d'email Thymeleaf aux couleurs de l'app.
- `Capacities` (port exact de `deriveCapacities`) + `CurrentUser` ; « Voir comme » stocké en session HTTP, bandeau de retour corrigé.
- Règles de rôle conformes à la base (`is_staff` = owner/setter/coach, `is_management` = owner/setter).
- Gardes : `/app/**` authentifié, groupes staff / owner, redirections identiques ; `OnboardingGate` (filtre ou intercepteur).
- **AppLayout complet** : sidebar (zones calculées depuis les capacités, repli, cookie, Ctrl/Cmd+B, drawer mobile), menu utilisateur,
  header (le bell et le bouton d'installation arrivent en M10/M15), `PageHeader`.
- `ConfigService` minimal (lecture `app_config` + `label(key, fallback)`) nécessaire aux libellés de navigation.
- Page **Équipe** `/app/admin/team` (ajout/retrait de rôles, owner non retirable).
- Owners amorcés par `APP_BOOTSTRAP_OWNER_EMAILS` (Marion — R1 — et Justine), sans aucune donnée personnelle dans le code.
- `/app` : page provisoire vide jusqu'à M9 (dashboards).
- Legacy : `pages/Auth.tsx`, `hooks/useAuth.tsx`, `lib/validation/*`, `components/auth/*`, `components/layout/*`, `components/ProtectedRoute.tsx`, `OnboardingGate.tsx`, `pages/app/owner/Team.tsx`, `services/authService.ts`.

## M2 — Configuration de l'application
- Page `/app/owner/settings` : 7 sections, accordéons, Enregistrer (insert/update), Réinitialiser (valeurs vidées).
- `LabelService` : `statusLabel`, `eventTypeLabel`, `videoLevelLabel`, `roleLabel`, `sourceLabel`, `prospectStatusLabel`, `prospectColLabel` ;
  `ObjectivesService` (`program_objectives` CSV).
- Legacy : `pages/app/owner/AppSettings.tsx`, `settings-config.ts`, `hooks/useAppConfig.tsx`, `useLabels.ts`, `useObjectives.ts`.

## M3 — Onboarding et Ma fiche
- `/onboarding` : stepper 4 étapes (état d'étape en Alpine, données envoyées en une fois ou par étape via HTMX), vidéo embed, chips,
  TagInput, disponibilités, préférences alimentaires JSON ; redirections.
- `/app/profile` : avatar (upload `StorageService`), infos, suivi sportif, `MemberSummaryForm`, notes du coach, enregistrer.
  Les cartes Documents / Documents partagés sont branchées en M4 et M11.
- Legacy : `pages/app/Onboarding.tsx`, `components/onboarding/*`, `pages/app/MyProfile.tsx`, `components/members/MemberSummaryForm.tsx`, `common/TagInput.tsx`, `ObjectiveSelector.tsx`.

## M4 — Membres (côté staff) et suivi
- `/app/coach/members` et `/app/admin/members` : recherche, filtres, badge coach (hash), création de membre (mot de passe temporaire + génération),
  édition (statut, coach, abonnement, téléphone), suppression.
- `/app/coach/members/:id` : en-tête, onglets Suivi / Coaching / Gestion (les sous-onglets Programme, Habitudes, Vidéos, Bilans affichent
  leur contenu au fil des modules M5–M8), résumé + notes coach, statut & abonnement.
- Suivi membre : `/app/tracking` (bilan hebdo, photos, mensurations, tendances) ; cartes photos et documents membres (`/app/profile` + fiche).
- `TrackingChartCard` (Chart.js selon Q6) : relatif/brut, périodes, séries, tuiles, CSV, comparaison de deux dates.
- Legacy : `pages/app/members/*`, `components/members/*`, `pages/app/MyTracking.tsx`, services `profiles`, `tracking`, `memberPhotos`, `memberDocuments`, `weeklyReviews`.

## M5 — Bilan PDF
- Dialog de génération (période, poids de départ/objectif, aperçu des deltas, sélection photos avant/après, upload ponctuel non persisté).
- `BilanCalculator` (port de `computeBilan`, testé), génération PDF serveur (openhtmltopdf), aperçu iframe, téléchargement,
  snapshot `member_reports` ; onglet « Bilans » (liste, suppression).
- **Modèle visuel cible = le PDF de référence fourni par la cliente** (« TON 1er MOIS DE TRANSFORMATION », 12 pages), et non le PDF A4 de
  l'ancienne app : format paysage 16:9 (900 × 507 pt), fond crème, titres capitales sans-serif (type Montserrat), corps serif, encarts
  taupe arrondis, graphiques en aires (poids avec ligne d'objectif, hanches, cuisses, bras), pages photos avant/après par vue avec
  4 points commentés, « Ce que ces résultats veulent dire », « Mais le plus important » (01–03), barre de progression vers l'objectif,
  « Sois fière de toi ». Le PDF de référence contient des données personnelles : il n'est **pas** versionné.
- À préciser au démarrage de M5 : polices exactes, illustrations (balance, mètre ruban, silhouette, pictogrammes), photos annotées
  (traits rouges/verts dessinés à la main dans l'exemple : hors périmètre sauf demande), textes variables selon la durée (1er mois, N semaines…).
- Legacy : `components/bilan/*`, `lib/bilan/*`, `services/memberReportsService.ts`.

## M6 — Vidéos
- `/app/coach/videos` (ajout, suppression, extraction ID YouTube), `/app/videos` (filtres, lecteur, `video_views`), historique vidéos de la fiche membre.
- Legacy : `pages/app/coach/VideoManager.tsx`, `pages/app/VideoLibrary.tsx`, `services/videosService.ts`.

## M7 — Entraînement
- Exercices (filtres, portées Tous/Mes/Favoris, attributs ≤5 parmi 14 types et leurs validations, duplication).
- Séances (blocs, conversion de type, tours, séries, attributs, combobox d'exercices, sauvegarde transactionnelle, duplication).
- Planifications (statuts, cible objectifs/élève, préremplissage, semaines/mois, dupliquer/monter/descendre, jours repos/séance/bilan,
  plusieurs séances par jour, duplication), hub `/app/coach/training?tab=`.
- Côté membre : `/app/planning` (semaine courante, onglets, marquer fait) ; fiche membre onglet Programme.
- Legacy : `pages/app/coach/{TrainingHub,ExerciseLibrary,SessionLibrary,SessionEditor,ProgramLibrary,ProgramEditor}.tsx`, `components/coach/{program/*,SessionExerciseRow,ExerciseAttributesEditor}.tsx`, `lib/programMapping.ts`, `types/{exercise,workoutSession,program}.ts`, `pages/app/MyPlanning.tsx`, `components/members/MemberProgramsCard.tsx`.

## M8 — Habitudes
- `/app/coach/habits` (CRUD, icônes, fréquence, globale, assignation), `MyHabitsCard` (cocher, série), suivi des habitudes sur la fiche membre (7/30/90 j, taux).
- Legacy : `pages/app/coach/HabitsLibrary.tsx`, `components/coach/AssignHabitDialog.tsx`, `components/members/{MyHabitsCard,MemberHabitsTrackingCard}.tsx`, `lib/habitIcons.ts`.

## M9 — Calendrier et tableaux de bord
- `/app/calendar` : liste, mois, dialog du jour, création d'événement, calendrier général owner filtrable par coach.
- `/app` : `StaffDashboard` (stats, agenda du jour, RDV, tâches, anniversaires + WhatsApp) et `MemberDashboard` (série, accomplies, coach,
  semaine, habitudes, liens rapides).
- Legacy : `pages/app/coach/StaffCalendar.tsx`, `pages/app/coach/calendar/*`, `pages/app/dashboard/*`, `components/members/BirthdaysWidget.tsx`, `lib/eventTypeConfig.ts`, `lib/coachBadgeConfig.ts`.

## M10 — Annonces
- `/app/coach/announcements` (onglets brouillons/programmées/publiées, formulaire markdown + aperçu, couverture, ciblage, récurrence stockée),
  `/app/announcements`, `NotificationBell` dans le header (non-lus, popover, marquer lu), règles de ciblage.
- Legacy : `pages/app/coach/Announcements.tsx`, `components/coach/announcements/*`, `pages/app/MyAnnouncements.tsx`, `components/layout/NotificationBell.tsx`, `lib/recurrence.ts`.

## M11 — Documents
- `/app/coach/documents` (filtres, glisser-déposer, visibilité, tags, membres autorisés), `SharedDocumentsCard` sur `/app/profile`,
  téléchargement autorisé selon la visibilité.
- Legacy : `pages/app/coach/DocumentsLibrary.tsx`, `components/members/SharedDocumentsCard.tsx`, `lib/fileIcons.ts`.

## M12 — Nutrition
- Aliments (table, filtres, OpenFoodFacts code-barres et recherche via `RestClient`), recettes (filtres, ingrédients), hub `?tab=`
  (vue d'ensemble, plans élèves, activité récente).
- Menus types (éditeur N semaines, grille hebdomadaire, assignation à un élève, compteur d'utilisation).
- Générateur 5 étapes (port exact de `mealPlanGenerator.ts` + `buildShoppingList`, testés sur des cas de référence) et envoi.
- `NutritionEditor` par membre, `/app/nutrition` côté membre (deux formats de `meals`, liste de courses, impression).
- Legacy : `pages/app/coach/{FoodLibrary,RecipeLibrary,MealPlanGenerator,NutritionEditor}.tsx`, `pages/app/coach/nutrition/*`, `components/coach/{meal-plan,nutrition,recipe}/*`, `RecipeFormDialog.tsx`, `lib/{mealPlanGenerator,recipeConfig}.ts`, `types/{nutrition,food,mealPlanTemplate}.ts`, `pages/app/MyNutrition.tsx`, `supabase/functions/openfoodfacts`.

## M13 — Ventes
- `/app/prospects` (stats, tableau filtrable, pipeline kanban, formulaire, suppression), conversion en membre (transactionnelle),
  `/app/calls`, `/app/my-booking-link` (placeholder identique).
- Legacy : `pages/app/admin/{Prospects,ProspectCard,ProspectFormDialog,ProspectsTable,ConvertProspectDialog,prospects-config}.tsx`, `pages/app/sales/*`, `supabase/functions/convert-prospect`, `lib/whatsapp.ts`.

## M14 — Finance
- `/app/money` : filtres, KPIs, alerte, 4 graphiques, table triable, édition financière, factures, rappel WhatsApp, export CSV.
- Legacy : `pages/app/admin/FinanceTracking.tsx`.

## M15 — Finitions et recette globale
- **Application installable sur téléphone (PWA)** : manifest complet, service worker minimal (page hors ligne), invite d'installation
  native sur Android/Chrome (`beforeinstallprompt` géré par Alpine), instructions iOS « Sur l'écran d'accueil » conservées ;
  vérification Lighthouse « installable ». Publication sur les stores (Play Store / App Store) hors périmètre sauf demande.
- Bouton « Installer l'app », vérification de toutes les redirections de compatibilité, accessibilité (focus, skip-link, aria),
  passe responsive complète, recette visuelle de bout en bout par rôle (owner, coach, setter, closer, manager, membre, membre externe).
- Pas d'import de données : démarrage sur une base vide, owners créés par inscription (Q4, Q9).
