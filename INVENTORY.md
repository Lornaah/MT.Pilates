# INVENTORY.md — M.T.Pilates (ancienne app React/Supabase)

Inventaire factuel de l'existant, établi à partir du code (`../legacy/mt.pilate.app-main`).
Rien n'est inventé : ce qui est absent, mort ou incohérent est signalé explicitement (⚠️).
Les questions ouvertes sont regroupées en fin de document (§9).

Sommaire : 1. Écrans et routes · 2. Rôles et permissions · 3. Entités et relations · 4. Règles RLS ·
5. Composants shadcn → Basecoat/Alpine · 6. Design tokens · 7. Intégrations externes ·
8. Code mort, bugs et incohérences · 9. Questions ouvertes

---

## 1. Écrans et routes

### 1.1 Gardes de navigation
| Garde | Règle | Échec |
|---|---|---|
| `ProtectedRoute` | utilisateur connecté | → `/auth` |
| `OnboardingGate` (sous `/app`) | membre non staff avec `profiles.onboarding_completed_at IS NULL` | → `/onboarding` |
| `StaffGate` | capacité `isStaff` (cf. §2) | → `/app` |
| `OwnerGate` | rôle effectif `owner` | → `/app` |

### 1.2 Table des routes
| Route | Écran | Accès |
|---|---|---|
| `/` | Landing publique (hero, Marion, approche, 3 piliers, CTA, footer) ; redirige vers `/app` si connecté | public |
| `/auth` | Connexion / Création de compte (onglets) | public |
| `/onboarding` | Assistant 4 étapes (Conditions, Vidéo, Sport, Nutrition) | connecté |
| `/app` | `DashboardRouter` : StaffDashboard si staff, sinon MemberDashboard | connecté |
| `/app/videos` | Bibliothèque vidéo membre (YouTube, filtres, lecteur en dialog) | connecté |
| `/app/tracking` | Mon suivi : bilan hebdo, photos d'évolution, mensurations | connecté |
| `/app/profile` | Ma fiche : infos, avatar, résumé, documents, documents partagés, notes coach | connecté |
| `/app/nutrition` | Mon plan nutritionnel actif + liste de courses | connecté |
| `/app/planning` | Mon programme (onglets Aujourd'hui / Cette semaine / Vue d'ensemble) ⚠️ absent de la sidebar | connecté |
| `/app/announcements` | Toutes mes annonces | connecté |
| `/app/calendar` | Calendrier staff (liste / mois ; owner : + calendrier général) | staff |
| `/app/coach/members` | Mes coachés (`scope=coach`) | staff |
| `/app/admin/members` | Tous les membres (`scope=admin`) ⚠️ pas d'entrée sidebar | staff |
| `/app/coach/members/:id` | Fiche membre (onglets Suivi / Coaching / Gestion) | staff |
| `/app/coach/members/:memberId/nutrition` | Éditeur de plan nutritionnel par repas | staff |
| `/app/coach/videos` | Gestion des vidéos | staff |
| `/app/coach/nutrition?tab=overview\|recipes\|foods\|templates` | Hub nutrition | staff |
| `/app/coach/nutrition/templates/new`, `/:id` | Éditeur de menu type | staff |
| `/app/coach/nutrition/plans/generate?member=` | Générateur de plan repas (wizard 5 étapes) | staff |
| `/app/coach/training?tab=programs\|sessions\|exercises` | Hub entraînement | staff |
| `/app/coach/exercises` | Bibliothèque d'exercices | staff |
| `/app/coach/sessions`, `/new`, `/:id` | Bibliothèque / éditeur de séances | staff |
| `/app/coach/programs`, `/new?assignee=`, `/:id` | Bibliothèque / éditeur de planifications | staff |
| `/app/coach/habits` | Habitudes | staff |
| `/app/coach/announcements` | Gestion des annonces | staff |
| `/app/coach/documents` | Bibliothèque de documents | staff |
| `/app/prospects` | Leads (tableau / pipeline kanban) | staff |
| `/app/calls` | Closing | staff |
| `/app/my-booking-link` | Placeholder « Mon lien » | staff |
| `/app/money` | Finance ⚠️ sidebar owner seulement, route ouverte à tout staff | staff |
| `/app/admin/team` | Équipe (gestion des rôles) | owner |
| `/app/owner/settings` | Configuration (libellés) | owner |
| `*` | 404 « Oops! Page introuvable » | public |

**Redirections de compatibilité** : `coach/recipes` → `coach/nutrition?tab=recipes` ; `coach/foods` → `?tab=foods` ;
`coach/meal-plans/generate` → `coach/nutrition/plans/generate` ; `admin/prospects` → `prospects` ; `admin/finance` → `money`.

### 1.3 Layout applicatif (`AppLayout`)
- Lien d'évitement « Aller au contenu principal ». Rangée `h-dvh` : Sidebar | colonne (ViewAsBanner, header sticky `h-12`, `main#main-content` avec animation `page-enter`).
- Header : `SidebarTrigger` (tooltip « Menu latéral ») à gauche ; `NotificationBell` et `InstallAppButton` à droite.
- **Sidebar** : repliable en icônes (16rem / 4rem), état persisté en cookie `sidebar:state` (7 jours), raccourci Ctrl/Cmd+B, drawer (`Sheet`) sous 768px, fermé à chaque navigation. En-tête logo (cercle `bg-gradient-warm`) « MT Pilates / Coach Pilates ».
- **Zones de navigation** (`buildNavigation`) :
  1. *Pratique* — membre externe : « Mon espace » (`/app`, `/app/program-readonly` ⚠️ route inexistante) ; membre : « Mon suivi » (`/app` « Mon programme », `/app/videos` « Mes séances », `/app/tracking` « Mon suivi », `/app/nutrition` « Nutrition », `/app/announcements` « Annonces »).
  2. *« Activité »* (staff) — Tableau de bord, Mon Calendrier, Annonces ; groupe « Coaching » (coach ou manager coach) : Mes coachés, Programme, Nutrition, Habitudes ; groupe « Bibliothèque » : Vidéos, Documents ; groupe « Ventes » (setter, closer, manager setter/closer) : Leads, Closing, + « Mon lien » si setter non owner.
  3. *« Administration »* (owner) — Finance, Équipe, Configuration.
  - Item actif : égalité stricte si `end`, sinon préfixe. Libellés surchargeables via `app_config` (`nav_*`).
- **Menu utilisateur** : avatar + email + libellé de rôle principal (priorité Owner > Manager coachs > Manager setters > Manager closers > Coach > Setter > Closer > Membre externe > Membre), lien `/app/profile` ; owner : select « Voir comme… » (Owner (moi), Coach, Setter, Closer, Manager coachs, Membre, Membre externe) ; bouton « Se déconnecter » → `/`.
- **ViewAsBanner** (bandeau ambre « Vous consultez l'application comme un **{label}**… », bouton « Revenir au mode owner »). ⚠️ ne s'affiche jamais (cf. §8).
- **NotificationBell** : vue `v_announcements_for_user`, pastille non-lus (« 9+ »), popover des 5 dernières (extrait markdown 140 car., date relative fr), clic = marquer lu, lien « Voir toutes les annonces → ».
- **InstallAppButton** : masqué en mode standalone ; dialog d'instructions (iOS vs autres), bouton « Compris ». Pas de service worker.
- **PageHeader** (eyebrow, titre `font-display`, description, action, bouton retour, avatar) ; **PageTransition** ; **ScrollReveal** (IntersectionObserver, landing uniquement).

### 1.4 Écrans publics et membre
- **Landing `/`** : textes, images (`hero-pilates.jpg`, `marion-pilates.jpg`, `feature-class.jpg`, `marion-coach.jpg`), animations `fade-up`/ScrollReveal, transition de sortie (opacité 0 + scale 0.985, 300 ms). Contenu intégralement statique.
- **Auth `/auth`** : panneau image (`auth-bg.jpg`) ; onglets « Connexion » / « Créer un compte » ; email pré-rempli depuis `localStorage.mt_remember_email` (« Se souvenir de moi ») ; afficher/masquer mot de passe.
  - Connexion : email (trim, lowercase, requis, format, ≤255), mot de passe (requis, ≤72). Succès : toast « Bienvenue ! » → `/app`. Erreurs traduites (email non confirmé, identifiants invalides, rate limit…). Si email non confirmé : encart + « Renvoyer l'email de confirmation ».
  - Inscription : Prénom (requis, ≤60), Nom (≤60), Email, Mot de passe (24–72 car., minuscule, majuscule, chiffre, spécial) avec jauge `PasswordStrengthMeter`. Succès : « Compte créé. Vérifiez votre email… » → onglet connexion. Rôle `member` attribué automatiquement.
  - ⚠️ Aucun « mot de passe oublié », aucun changement de mot de passe dans l'app.
- **Onboarding** : stepper ; (1) 2 cases CGU + renonciation rétractation (non stockées, seul `terms_accepted_at`) ; (2) vidéo `app_config.onboarding_video_url` (YouTube/Vimeo → embed) ; (3) profil sportif : identité, objectifs (chips `program_objectives`), taille, poids, séances/sem., sédentarité, morphotype, pathologies, historique, matériel (20 chips), disponibilités (7 jours × Matin/Midi/Soir) ; (4) nutrition : régime (8 chips), allergies, aliments aimés/pas aimés, repas/jour (1–8), budget, temps de cuisine, notes → `profiles` + `dietary_preferences` JSON + `onboarding_completed_at`.
- **MemberDashboard** : « Bonjour, *{prénom}* » ; cartes « Série en cours » (jours consécutifs avec vue vidéo, aujourd'hui inclus), « Accomplies » (vues du mois / `weekly_target_sessions × 4`), « Ton coach » ; bandeau semaine (7 jours, événements) + « Voir tout » → `/app/planning` ; `MyHabitsCard` (cocher l'habitude du jour, série) ; 3 liens rapides.
- **VideoLibrary** : vidéos publiées, recherche titre/focus, filtre niveau, carte miniature YouTube, dialog lecteur `youtube.com/embed/{id}?autoplay=1&rel=0` + insertion `video_views`.
- **MyTracking** : bilan hebdo (`weekly_reviews`, bien-être 1–10, upsert par semaine) ; photos d'évolution (face/profil/dos) ; entrées de mensurations (poids, masse grasse, tours taille/hanches/poitrine/bras/cuisse, notes) avec tendance vs entrée précédente ; ajout / suppression.
- **MyProfile** : avatar (upload), infos, suivi sportif, `MemberSummaryForm` (« Mon résumé »), `MemberDocumentsCard`, `SharedDocumentsCard` (documents visibles, lien signé 5 min), « Notes de votre coach » (lecture), bouton « Enregistrer ».
- **MyNutrition** : plan actif le plus récent ; objectifs journaliers (si non masqués) ; 7 cartes jour, deux formats de `meals` (généré : liste de `PlannedMeal` ; legacy : `{breakfast,lunch,snack,dinner}`) ; liste de courses + « Imprimer » ; « Télécharger le PDF » si `pdf_url`.
- **MyPlanning** : programme `active` assigné (ou commun) le plus récent ; semaine courante calculée depuis `start_date` ; cartes jour (type, séances, note du coach en popover, « Marquer fait » / « Fait ✓ » → `program_day_completions`).
- **MyAnnouncements** : liste markdown, badge « Nouveau », clic = lu.

### 1.5 Écrans staff — coaching
- **StaffDashboard** : « Bonjour {prénom} » ; StatCards Membres actifs / En pause / Archivés / Total ; « Agenda du jour » ; « Prochains RDV individuels » ; « Tâches admin à venir » ; `BirthdaysWidget` (7 jours, lien WhatsApp pré-rempli).
- **MembersList** : recherche, filtres coach (admin) et statut ; ligne avatar, nom, email, abonnement, téléphone, badge coach coloré (hash), statut ; owner en scope admin : « Nouveau membre » (création de compte avec mot de passe temporaire + génération), éditer (statut, coach, dates abo, téléphone), supprimer (profil uniquement).
- **MemberDetail** : en-tête (avatar, « Générer un bilan PDF ») ; onglet **Suivi** (Résumé + notes coach privées, Mensurations = graphique d'évolution + photos, Bilans PDF) ; **Coaching** (Programme, Habitudes avec taux de complétion 7/30/90 j, Documents, historique vidéos) ; **Gestion** (statut, coach, dates abo — modifiables si manager, sauvegardés par le bouton du Résumé ⚠️).
- **TrackingChartCard** : courbes relatives (base 100) ou brutes, période 1/3/6/12 mois/tout, séries activables, tuiles de tendance (bon = baisse poids/taille, hausse sinon), export CSV, dialog « Comparer deux dates ».
- **Bilan PDF** : période, poids de départ/objectif, aperçu des deltas, sélection photos avant/après par côté, aperçu iframe, téléchargement (`bilan-{prénom}-{nom}-{fin}.pdf`) + enregistrement d'un snapshot `member_reports` ; PDF A4 multi-pages au texte fixe (couverture, chiffres, photos, courbe de poids, détails par mensuration, signification, progression vers l'objectif, conclusion). Liste des bilans (suppression).
- **Entraînement** : `ExerciseLibrary` (filtres muscles/équipement/type/objectifs, Tous/Mes exercices/Favoris, attributs par défaut ≤5 parmi 14 types, duplication RPC) ; `SessionLibrary`/`SessionEditor` (blocs single/superset/triset/circuit, tours, séries, attributs, notes ; sauvegarde atomique RPC `save_workout_session`) ; `ProgramLibrary`/`ProgramEditor` (statuts, cible objectifs ou élève, semaines groupées par mois, jours repos/séance/bilan, plusieurs séances par jour, duplication).
- **HabitsLibrary** : CRUD habitudes (icône parmi 21, fréquence, globale), assignation aux membres actifs.
- **VideoManager** : ajout (titre, URL/ID YouTube, durée, niveau, focus, description) et suppression. Pas d'édition.
- **Announcements** : onglets Brouillons (date ≥ 2099) / Programmées / Publiées ; formulaire (markdown + aperçu, image de couverture, canal, date de publication, cible all/active/specific/by_coach, récurrence DAILY/WEEKLY(+jours)/MONTHLY stockée seulement).
- **DocumentsLibrary** : filtres visibilité, catégories, tags ; dépôt par glisser-déposer ; visibilité staff_only/all_members/specific.
- **StaffCalendar** : vue liste / mois, dialog du jour, création d'événement (Suivi individuel / Premier RDV / Appel, +1 h, privé) ; owner : « Calendrier général » filtrable par coach. Pas d'édition d'événement.
- **Nutrition** : Hub (vue d'ensemble KPIs, plans élèves, activité récente) ; Recettes (filtres régime/allergène/équipement/type de repas, ingrédients) ; Aliments (table, import OpenFoodFacts code-barres et recherche texte) ; Menus types (couleur, kcal, macros, N semaines × 7 jours) + assignation à un élève ; Générateur (algorithme glouton déterministe, liste de courses agrégée) ; NutritionEditor (plan par repas, import d'ingrédients de recettes).

### 1.6 Écrans staff — ventes, finance, administration
- **Prospects** : StatCards ; vue tableau (filtres, WhatsApp, inscrire, modifier) et pipeline kanban (changement de statut, convertir, modifier, supprimer) ; formulaire prospect ; conversion en membre (mot de passe ≥ 8 car., coach) via edge function.
- **Calls** : RDV planifiés, à rappeler, en essai à conclure.
- **MyBookingLink** : placeholder « Lien en cours de configuration ».
- **FinanceTracking** : filtres, KPIs (abonnés actifs, CA contracté/collecté, reste), alerte, 4 graphiques (CA 12 mois, CA par source, recouvrement, abonnés par statut), table triable, édition financière, factures (documents catégorie « Facture »), rappel WhatsApp, export CSV `;` + BOM.
- **Team** : utilisateurs ayant au moins un rôle staff ; ajout/retrait de rôles (owner non retirable).
- **AppSettings** : 7 sections de libellés (`nav_*`, `role_label_*`, `status_label_*`, `prospect_*`, `source_label_*`, `video_level_*`, `event_type_*`, `page_title_*` ⚠️ jamais lus, `onboarding_video_url`) ; Enregistrer / Réinitialiser (valeurs vidées → libellés par défaut). ⚠️ `program_objectives` et `session_categories` non éditables ici.

Le détail exhaustif par écran (champs, libellés exacts, toasts, formules) sera relevé dans le code source au démarrage de chaque module ; `PLAN.md` liste les fichiers de référence par module.

---

## 2. Rôles et permissions

### 2.1 Rôles (`app_role`)
`owner`, `coach`, `setter`, `closer`, `manager_coach`, `manager_setter`, `manager_closer`, `member`, `external_member`
(+ `admin`, `user` : historiques, inutilisés). Un utilisateur peut cumuler plusieurs rôles (`user_roles`, unique `(user_id, role)`).
Inscription → rôle `member`. Le premier owner est attribué en base (aucune UI).

### 2.2 Capacités côté interface (`deriveCapacities`)
| Capacité | Règle |
|---|---|
| `isOwner` | owner |
| `isCoach` / `isSetter` / `isCloser` | rôle correspondant ‖ owner |
| `isManagerCoach/Setter/Closer` | rôle manager correspondant ‖ owner |
| `isManager` | un des managers ‖ owner |
| `isStaff` | coach ‖ setter ‖ closer ‖ manager ‖ owner |
| `isExternalMember` | external_member |
| `isMember` | member ‖ (ni staff ni externe) |
| `canManageCoaches/Setters/Closers` | manager correspondant ‖ owner |

**Voir comme** (owner) : rôles effectifs remplacés par `[rôle choisi]`, en mémoire (non persisté).

### 2.3 Fonctions de sécurité côté base (font foi pour les données)
| Fonction | Définition finale |
|---|---|
| `has_role(u, r)` | u possède r |
| `is_staff(u)` | **owner, setter, coach** |
| `is_management(u)` | **owner, setter** |
| `can_manage_member(u, m)` | `is_management(u)` ‖ `profiles(m).coach_id = u` |

⚠️ Divergence majeure : l'UI considère closer et managers comme staff, la base non (ils ouvrent les écrans mais les requêtes renvoient vide / échouent). Le setter est « management » en base (voit tous les profils, prospects, plans…). Voir question Q1.

---

## 3. Entités et relations (état final après 38 migrations)

Enums Postgres : `app_role`, `member_status` (active, paused, archived, finished), `event_type` (live_class, studio_class, one_on_one, call, admin_task), `video_level` (beginner, intermediate, advanced), `prospect_status` (new, to_call, trial, converted, lost), `prospect_source` (instagram, tiktok, website, word_of_mouth, referral, other).
Toutes les tables ont `id uuid` et la plupart `created_at`/`updated_at` (trigger).

| Table | Colonnes clés | Relations |
|---|---|---|
| `auth.users` (Supabase) | email, mot de passe, confirmation, meta prénom/nom | → remplacée par une table `users` applicative |
| `profiles` | email, first/last_name, phone, date_of_birth, avatar_url, bio, goals, height_cm, weight_kg, measurements jsonb (inutilisé), weekly_target_sessions (2), coach_notes, status, subscription_start/end_date, coach_id, allergies[], pathologies[], work_sedentarity (CHECK), sport_history, body_type (CHECK), coaching_motivation, external_summary, dietary_preferences jsonb, onboarding_completed_at, terms_accepted_at, objectives[], available_equipment[], availability jsonb, billing_source, contract_amount, paid_amount, payment_installments, billing_notes | 1-1 user ; coach_id → user |
| `user_roles` | user_id, role | n-1 user |
| `app_config` | key (unique), value | — |
| `videos` | title, description, youtube_id, thumbnail_url, duration_minutes, level, focus_areas[], tags[], is_published, created_by | |
| `video_views` | user_id, video_id, watched_at, completed, rating 1–5, notes | → user, video |
| `events` | title, description, type, starts_at, ends_at, capacity, location, meeting_url, coach_id, member_id, is_private, created_by | |
| `event_participants` | event_id, user_id, attended, registered_at | ⚠️ aucune UI |
| `member_tracking` | user_id, date, weight_kg, body_fat_pct, waist/hip/chest/arm/thigh_cm, notes | |
| `member_photos` | user_id, taken_at, photo_url, side (front/side/back), notes | |
| `member_documents` | user_id, uploaded_by, title, category, file_url, file_size_bytes, mime_type, uploaded_at | |
| `member_reports` | user_id, created_by, period_start/end, target_weight_kg, start_weight_override_kg, snapshot jsonb, notes | |
| `weekly_reviews` | user_id, week_start_date (unique avec user), mental_wellbeing 1–10, free_note | |
| `habits` | title, description, icon, frequency (daily/weekly/custom), is_global, created_by | |
| `habit_assignments` | habit_id, user_id (unique) | |
| `habit_completions` | habit_id, user_id, completed_date (unique) | |
| `announcements` | title, body (markdown), cover_image_url, channel (in_app/email/push/all), target (all/active/specific/by_coach), target_user_ids[], target_coach_id, is_recurring, recurrence_rule, publish_at, created_by | |
| `announcement_reads` | announcement_id, user_id (unique), read_at | |
| `v_announcements_for_user` (vue) | annonces ciblées publiées + read_at | |
| `documents` | title, description, category (guide/fiche_pratique/contrat/recette/autre), tags[], file_url (chemin), file_size_bytes, mime_type, visibility (staff_only/all_members/specific), shared_with_ids[], uploaded_by | |
| `exercises` | title, description, video_url, demo_url, thumbnail_url, muscle_groups[], equipment[], exercise_type, default_attributes jsonb (≤5), objectives[], is_favorite (global), is_published, created_by | |
| `workout_sessions` | title, description, category, duration_minutes, level, is_template, is_published, created_by | |
| `workout_session_blocks` | session_id, position (unique), block_type (single/superset/triset/circuit), rounds, notes | cascade |
| `workout_session_exercises` | block_id, exercise_id (RESTRICT), position_in_block, attributes jsonb (≤5), sets, exercise_note | cascade |
| `programs` | title, description, status (draft/scheduled/active/finished/template), assignee_id, start_date, duration_weeks, objectives[], created_by | |
| `program_weeks` | program_id, week_number (unique), week_comment | cascade |
| `program_days` | program_week_id, day_of_week 1–7 (unique), day_type (workout/rest/review/nutrition/mixed), day_comment, workout_session_id (legacy), nutrition_plan_id | cascade |
| `program_day_sessions` | program_day_id, workout_session_id, position | cascade |
| `program_day_completions` | program_day_id, user_id (unique), completed_at, member_notes | |
| `nutrition_plans` | member_id, coach_id, title, description, meals jsonb (2 formats), starts_on, ends_on, is_active, shopping_list jsonb, pdf_url, sent_at, kcal_target, macros_target jsonb, generated_from_template, hide_macros_from_member, source_template_id | → meal_plan_templates |
| `meal_plan_templates` | title, description, status (draft/published/archived), cover_color, kcal_target, macros_target, meals_per_day 1–8, dietary_preferences, duration_weeks 1–12, weeks jsonb, usage_count, created_by | |
| `recipes` | title, description, category, ingredients jsonb, instructions, calories, protein/carbs/fat_g, source, external_id/url, image_url, video_url, prep/cook_time_minutes, servings, dietary_tags[], allergens[], equipment_needed[], is_dessert, meal_types[], created_by | |
| `foods` | name, brand, barcode, serving_size_g, calories/protein/carbs/fat/fiber per 100g, source (manual/open_food_facts/ciqual/custom_coach), external_id, aisle (9 valeurs), is_public, created_by | |
| `prospects` | first/last_name, email, phone, age, source, status, tag, notes, objectives, next_followup_at, appointment_at, assigned_setter_id ⚠️, assigned_closer_id ⚠️, converted_member_id, created_by | → profiles |

**Fonctions métier (RPC)** : `save_workout_session(payload)`, `duplicate_workout_session`, `duplicate_exercise` (⚠️ ne copie pas `objectives`), `duplicate_program` (owner ou coach), `apply_template` (⚠️ inutilisée), `bump_meal_plan_template_usage`.
Elles deviennent des méthodes `@Transactional` de service.

**Buckets de stockage** : `avatars` (public, `{uid}/avatar.{ext}`), `member-photos` (privé, `{uid}/…`), `member-documents` (privé, `{uid}/…`), `documents` (privé, `{uuid}.{ext}`), `announcement-images` (public, `{uuid}.{ext}`). Le bucket `meal-preps` a été abandonné.

---

## 4. Règles RLS → règles d'autorisation de service

Notation : `self` = la ligne appartient à l'utilisateur ; `staff` = `is_staff` ; `mgmt` = `is_management` ; `manage(m)` = `can_manage_member`.

| Ressource | Lecture | Création | Modification | Suppression |
|---|---|---|---|---|
| profiles | self ‖ mgmt ‖ `coach_id = moi` | mgmt | self ‖ mgmt ‖ `coach_id = moi` | mgmt |
| user_roles | self ‖ staff | owner | — | owner |
| app_config | tous (y compris anonyme) | owner | owner | owner |
| videos | publiée ‖ staff | staff | staff | staff |
| video_views | self ‖ staff | self | self | — |
| events | owner ‖ coach_id = moi ‖ created_by = moi ‖ member_id = moi | staff | staff | staff |
| event_participants | self ‖ staff | self ‖ staff | staff | self ‖ staff |
| member_tracking | self ‖ staff | self | self | self |
| member_photos / member_documents | self ‖ manage | self ‖ manage | self ‖ manage | self ‖ manage |
| weekly_reviews | self ‖ manage | self | self | self ‖ manage |
| member_reports | self ‖ manage | manage | manage | manage |
| habits | tous | staff | staff | staff |
| habit_assignments | self ‖ manage | staff ∧ manage | — | staff ∧ manage |
| habit_completions | self ‖ manage | self | — | self ‖ manage |
| announcements | staff ; sinon publiée ∧ ciblée (all / active si statut actif / specific si dans la liste / by_coach si mon coach) | staff | staff | staff |
| announcement_reads | self ‖ staff | self | — | self |
| documents | staff ‖ all_members ‖ (specific ∧ moi ∈ shared_with_ids) | staff | staff | staff |
| exercises / workout_sessions (+ blocs, exercices) | publié ‖ staff | staff | staff | staff |
| programs (+ semaines, jours) | staff ‖ assignee = moi ‖ assignee nul | staff ∧ (assignee nul ‖ manage) | idem | idem |
| program_day_sessions | tous | owner ‖ coach | owner ‖ coach | owner ‖ coach |
| program_day_completions | self ‖ staff | self | self | self ‖ staff |
| nutrition_plans | membre = moi ‖ manage | manage | manage | manage |
| meal_plan_templates | staff | staff | staff | staff |
| recipes | tous | staff | staff | staff |
| foods | public ‖ staff | staff | staff | staff |
| prospects | mgmt | mgmt | mgmt | mgmt |
| Fichiers avatars / announcement-images | publics | self / staff | self / staff | self / staff |
| Fichiers member-photos / member-documents | dossier = moi ‖ manage(dossier) | idem | — | idem |
| Fichiers documents | staff ‖ document visible référençant le fichier | staff | staff | staff |
| Conversion prospect (edge function) | — | mgmt | — | — |

Mise en œuvre prévue : un composant `AccessPolicy` par feature (méthodes `canRead/canWrite…` pures et testées),
appliqué dans les services ; les listes sont filtrées en requête (pas de chargement total puis filtrage).

---

## 5. Composants shadcn utilisés → équivalent Basecoat / Alpine

Nombre de fichiers consommateurs entre parenthèses (hors `components/ui`). Les composants shadcn présents mais jamais importés
(accordion, avatar, breadcrumb, calendar, carousel, chart, collapsible, context-menu, drawer, dropdown-menu, form, hover-card,
input-otp, menubar, navigation-menu, pagination, progress, radio-group, resizable, toggle, toggle-group) ne seront pas portés.

| shadcn / lib | Usage | Équivalent cible |
|---|---|---|
| Button (75) — variantes `default, destructive, outline, secondary, ghost, link, hero, outlineLight, cream, sage` ; tailles `default, sm, lg, icon` ; `rounded-full` | partout | `.btn` Basecoat + classes de variantes custom dans `app.css` |
| Card (53) | partout | `.card` Basecoat (radius `1rem`) |
| Badge (50) — `default, secondary, destructive, outline, success, warm, pop, coach, sage, bordeaux` | statuts, tags | `.badge` Basecoat + variantes custom |
| Select (39) | filtres, formulaires | `<select class="select">` natif ; select stylé Basecoat + Alpine si rendu custom nécessaire (ex. items en badges) |
| Input (39), Label (37), Textarea (15), Checkbox (6), Switch (7) | formulaires | `.input`, `.label`, `.textarea`, `.checkbox`, `.switch` (`input[role=switch]`) Basecoat |
| Slider (1) | générateur kcal | `<input type=range class="slider">` |
| Tabs (14) | hubs, fiches, onglets URL | `.tabs` Basecoat + Alpine (`x-data`) ; onglets synchronisés à `?tab=` rendus serveur (liens `hx-get` + `hx-push-url`) |
| Dialog (10), FormDialog | formulaires modaux | `<dialog class="dialog">` + Alpine ; contenu chargé par HTMX |
| AlertDialog (1) via ConfirmDialog | suppressions | `<dialog class="dialog">` de confirmation + `hx-delete` |
| Popover (8) | filtres, notes, bell | `.popover` Basecoat + Alpine (`@click.outside`) |
| Command / cmdk (3) | combobox séances / exercices / recettes | combobox Alpine + recherche `hx-get` (fragment de résultats) |
| Tooltip (4) | sidebar repliée, boutons | attribut `data-tooltip` Basecoat |
| ScrollArea (4) | listes | `overflow-auto` + scrollbar stylée |
| Table (3) | aliments, prospects, finance | `.table` Basecoat ; tri via liens HTMX |
| Sidebar (3) (+ Sheet, Separator, Skeleton internes) | layout | `.sidebar` Basecoat + Alpine (repli, cookie `sidebar:state`, Ctrl/Cmd+B, drawer mobile) |
| Toast / Sonner | retours d'action | `.toast` Basecoat piloté par Alpine, alimenté par `HX-Trigger` |
| M3DatePicker / M3DateTimePicker (10) (react-day-picker) | dates | composant Thymeleaf + Alpine reproduisant le panneau (mois/année, grille lundi, « Aujourd'hui », heure pas de 15 min) — **pas** de dépendance |
| Skeleton (recettes) | chargement | `animate-pulse` Tailwind |
| recharts (3) : LineChart, BarChart, PieChart | suivi, finance | ⚠️ nouvelle dépendance à valider (cf. Q6) |
| react-markdown (4) | annonces | rendu Markdown serveur ⚠️ dépendance à valider (Q6) |
| @react-pdf/renderer (1) | bilan PDF | génération PDF serveur ⚠️ dépendance à valider (Q6) |
| lucide-react | icônes partout | sprite SVG Lucide (fichiers SVG vendus, licence ISC) via fragment `icon(name)` |
| date-fns + locale fr | formats de dates | `java.time` + `DateTimeFormatter` `Locale.FRENCH` ; `formatDistanceToNow` réimplémenté |
| react-hook-form + zod | validation | Bean Validation + validateurs custom (mot de passe, EPCP, plages) |
| TanStack Query | cache client | sans objet (rendu serveur) |

Composants communs à recréer en fragments : `PageHeader`, `StatCard` (tons default/primary/accent/sage/dark/secondary), `StatusBadge`,
`EmptyState`, `FormDialog`, `ConfirmDialog`, `FormField`, `TagInput`, `ObjectiveSelector`, `UserAvatar` (sm/md/lg/xl), `MemberLink`,
`LoadingSpinner`, `PasswordStrengthMeter`, `Stepper`.

---

## 6. Design tokens (à reprendre à l'identique)

- **Polices** : Fraunces (opsz 9..144, 300–600) pour `h1–h4` et `font-display` ; Inter (300–600) pour le corps ; `font-feature-settings: "ss01", "cv11"`. Chargées depuis Google Fonts.
- **Couleurs (HSL, `:root`)** : background `38 44% 96%` (crème), foreground `0 50% 14%` (bordeaux profond), card/popover `38 50% 98%`, primary `22 82% 38%` (terracotta), primary-glow `30 70% 58%`, secondary `200 35% 28%`, muted `28 30% 88%` / `0 25% 30%`, accent `30 65% 60%`, sage `95 22% 62%`, coach `168 55% 32%`, setter `265 50% 48%`, trial `38 70% 50%`, bordeaux `0 70% 22%`, destructive `0 70% 35%`, success `150 45% 30%`, border/input `28 25% 82%`, ring `22 82% 38%`, sidebar (8 variables). Toutes les paires `*-foreground` reprises.
- **Thème sombre** `.dark` défini mais **jamais activé** (aucun sélecteur de thème) → non porté (cf. Q8).
- **Radius** `--radius: 1rem` ; `lg = 1rem`, `md = calc(1rem - 4px)`, `sm = calc(1rem - 8px)`.
- **Dégradés** `gradient-warm`, `gradient-soft`, `gradient-hero` ; **ombres** `soft`, `warm`, `elevated` ; texture `.grain`.
- **Animations** `accordion-down/up`, `fade-up` (0.8 s), `fade-in` (0.6 s), `breathe` (6 s), `page-enter` (0.35 s) ; `tailwindcss-animate` et `@tailwindcss/typography` (`prose` pour le markdown).
- **Container** centré, padding `1.5rem`, `2xl: 1320px`. Focus visible `outline-ring`, lien d'évitement `.skip-link`.
- **Couleurs en dur** à reprendre : palette coach (hash ×31 mod 8), couleurs de semaines du ProgramEditor (emerald/amber/sky/fuchsia/rose), couleurs de types de repas (#FAEEDA…), couleurs de cover des menus types, séries de graphiques (#8884d8…), palette PDF (#FBF6EE, #3B2A1F, #B85A2E, #9CB48F, #F5EBD9), PWA `#E8927C` / `#FFF7F5`.
- **Assets** : `logo-icon.png`, 5 photos JPG, `favicon.png`, `pwa-icon-192/512.png`, `manifest.json` (start_url `/app`, standalone).

---

## 7. Intégrations externes

| Intégration | Usage actuel | Cible |
|---|---|---|
| Supabase Auth | email + mot de passe, confirmation d'email, renvoi de confirmation, `admin.createUser` (conversion prospect) | Spring Security form login + session, BCrypt ; confirmation d'email : cf. Q3 |
| Supabase Storage | 5 buckets, URLs publiques ou signées (5 min / 365 j) | `StorageService` local ; fichiers servis par un controller qui applique les règles §4 |
| Edge function `convert-prospect` | crée le compte, met à jour profil + prospect (non transactionnel) | méthode de service `@Transactional` |
| Edge function `openfoodfacts` | proxy `world.openfoodfacts.org` (code-barres, recherche) | client `RestClient` Spring (aucune dépendance) |
| Edge function `dispatch-announcement` | stub, jamais appelé | non porté (aucun envoi email/push n'existe) |
| YouTube / Vimeo | miniatures `i.ytimg.com`, iframes embed | identique |
| WhatsApp | liens `wa.me/{numéro}?text=` (anniversaires, relances, prospects) | identique |
| Google Fonts | Fraunces + Inter | identique |
| PWA | manifest + icônes, pas de service worker | identique |
| MCP Lovable (`list-members`) | outil non importé par l'app | non porté (cf. Q10) |

---

## 8. Code mort, bugs et incohérences relevés

**Sécurité / accès**
1. `isStaff` UI ≠ `is_staff` base (closer, managers) ; prospects lisibles seulement par owner + setter alors que closers/managers ont le menu « Ventes ».
2. `/app/money` accessible par URL à tout staff (seule la sidebar le restreint à l'owner).
3. `openfoodfacts` sans contrôle d'authentification.
4. Mots de passe : 24 caractères à l'inscription / création par l'owner, 8 à la conversion de prospect.

**Bugs fonctionnels**
5. `ViewAsBanner` ne s'affiche jamais en impersonation et le sélecteur « Voir comme » disparaît : seul un rechargement ramène en mode owner.
6. `/app/program-readonly` (sidebar membre externe) → 404.
7. `MyPlanning` renvoie le membre vers `/app/coach/sessions/:id` (route staff) → redirigé vers `/app`.
8. `NutritionEditor` lit le format legacy de `meals` alors que générateur et menus types écrivent le format « généré » : ouvrir puis enregistrer écrase le plan.
9. `ExerciseLibrary` : l'édition remet `is_favorite` à false et écrase `created_by` ; favori global, non par utilisateur.
10. `created_by` jamais renseigné pour recettes, aliments, habitudes → onglet « Mes recettes » toujours vide.
11. Doubles toasts (onboarding, envoi de plan, suppression de bilan) ; suppression de bilan par `confirm()` natif.
12. MemberDetail : l'onglet Gestion n'a pas de bouton d'enregistrement propre ; `MemberSummaryForm` titré « Mon résumé » même côté coach.
13. Suppression de photos / documents : fichiers laissés dans le stockage.
14. URLs signées 365 j stockées en base pour photos et documents membres (expirent).
15. MemberPlansList : filtre « Brouillons » jamais satisfait ; « Partir d'un menu et adapter » = même lien qu'« Assigner ».
16. Générateur : « Régénérer » un jour redonne le même résultat (déterministe) ; liste de courses en un seul rayon « autre ».
17. Finance : « Reste à percevoir » non borné alors que le graphique de recouvrement borne à 0 par membre ; sources affichées en anglais capitalisé (« Word Of Mouth »).
18. Team : un membre pur n'apparaît jamais, donc ne peut pas être promu depuis l'UI.
19. `MembersList scope=coach` filtre toujours sur « mes coachés », même pour owner/manager.
20. M3DatePicker : impossible de vider une date une fois saisie.

**Code mort / placeholders**
21. `MyBookingLink` (placeholder), export PDF de plan repas (texte « prochaine mise à jour »), `dispatch-announcement` (stub), récurrence d'annonces stockée mais jamais appliquée, `apply_template`, `event_participants`, `profiles.measurements`, `assigned_setter_id/closer_id`, `page_title_*`, `downloadInvoices`, `/app/admin/members` sans entrée de menu, statut `template` jamais posé depuis l'éditeur, `day_type = nutrition` jamais posé, `meals_per_day` du menu type inutilisé, `is_dessert` jamais éditable.

---

## 9. Décisions (validées le 2026-11-10)

| # | Décision |
|---|---|
| Q1 | Les règles d'accès de la **base** font foi (§2.3, §4). Les closers et managers gardent leurs menus mais n'accèdent qu'aux données que la base leur ouvrait. |
| Q2 | Corriger les failles d'accès (§8 n° 1–3, en restant conforme à Q1) et les bugs bloquants (§8 n° 5–8, 13, 14). Reproduire le reste à l'identique. |
| Q3 | Inscription simplifiée : **email + mot de passe → email de vérification → le compte est créé au clic sur le lien** (inscription en attente tant que non confirmée). Prénom et nom ne sont plus demandés à l'inscription : l'onboarding les collecte déjà (étape Sport). Le « renvoyer l'email de confirmation » est conservé. Nécessite `spring-boot-starter-mail` + un SMTP. |
| Q4 | **Aucune reprise de données.** Seuls deux comptes owner sont prévus : Marion (MT Pilates) et Justine. Ils sont configurés hors code (cf. Q9). |
| Q5 | Comparaison visuelle sur l'instance existante avec un **compte de test**. |
| Q6 | Dépendances validées : Thymeleaf Layout Dialect, thymeleaf-extras-springsecurity6, Chart.js, commonmark-java, openhtmltopdf. Plus `spring-boot-starter-mail` (Q3). |
| Q7 | Tailwind **v4** + Basecoat, buildé via frontend-maven-plugin. |
| Q8 | Pas de thème sombre livré pour l'instant, mais l'architecture doit permettre d'**ajouter un thème (sombre, Noël…) sans toucher aux templates** (cf. `CLAUDE.md`, section Thèmes). Les tokens `.dark` de l'original sont portés tels quels pour servir de base. |
| Q9 | Comptes owner amorcés par configuration (`app.bootstrap.owner-emails`, variable d'environnement, jamais commitée). Quand un de ces emails termine son inscription, il reçoit le rôle `owner`. Le rôle `admin` historique n'est pas réutilisé. |
| Q10 | Hors périmètre : l'outil MCP Lovable, `dispatch-announcement`, les clés `page_title_*`, `apply_template`, `event_participants`. |
| Q11 | Les URL existantes et le package `fr.mtpilates` sont conservés. |

### Questions restantes
- **R1** — ✅ Email de Marion communiqué. Il sera renseigné uniquement dans `APP_BOOTSTRAP_OWNER_EMAILS` (fichier `.env` local non commité et variable de déploiement), jamais dans le code ni dans la doc.
- **R2** — ✅ Fournisseur SMTP au choix de l'implémentation : configuration SMTP standard (`SPRING_MAIL_*`), indépendante du fournisseur. Recommandation : **Brevo** (offre gratuite 300 emails/jour, société française, conforme RGPD). Il faudra créer le compte et vérifier le domaine d'envoi (SPF/DKIM) au déploiement. En dev, les emails sont capturés localement par Mailpit (conteneur docker-compose, aucune dépendance Maven) ; en test, par un faux `JavaMailSender`.
- **R3** — Compte(s) de test pour la comparaison visuelle : idéalement un par rôle (owner, coach, setter, membre). La transmission sécurisée des identifiants sera organisée au premier module concerné (M0), sans les écrire dans le chat.
- Note : `TODO.md` signale que la connexion échoue avec les emails contenant un « . ». La nouvelle authentification ne réutilise pas ce code ; un test couvrira ce cas.

## 10. Questions initiales (archivées)

- **Q1 — Source de vérité des permissions.** L'UI ouvre les écrans staff aux closers et managers, la base les en prive ; le setter a des droits « management » en base. Reproduire strictement la base (closers/managers voient des écrans vides) ou aligner la base sur l'UI ? *Recommandation : reproduire les règles base à l'identique, sauf décision contraire de votre part rôle par rôle.*
- **Q2 — Bugs relevés (§8).** Reproduire tels quels, ou corriger ? *Recommandation : corriger les failles d'accès (1–3) et les bugs bloquants (5–8, 13–14), reproduire le reste à l'identique ; liste à valider.*
- **Q3 — Confirmation d'email.** Supabase envoie un email de confirmation à l'inscription et bloque la connexion avant confirmation. Le conserver nécessite un envoi d'email (`spring-boot-starter-mail` + SMTP). Conserver, ou activer les comptes immédiatement ?
- **Q4 — Reprise des données.** Faut-il migrer les données de production Supabase (utilisateurs avec leurs hash bcrypt, profils, fichiers des buckets) ? Si oui, je prévois un module d'import dédié. *Le schéma Flyway sera de toute façon un schéma consolidé (état final des 38 migrations, sans le schéma `auth`).*
- **Q5 — Comparaison visuelle.** L'ancienne app ne tourne qu'avec un backend Supabase. Pour les captures côte à côte : (a) Supabase local (CLI + Docker) avec les migrations et un jeu de données de démo, (b) un compte de test sur l'instance existante, (c) captures fournies par vous ?
- **Q6 — Nouvelles dépendances à valider.**
  - Thymeleaf Layout Dialect (`nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`) — layouts ;
  - `thymeleaf-extras-springsecurity6` — `sec:authorize` dans les templates ;
  - Testcontainers (`postgresql`, `junit-jupiter`) et Flyway (`flyway-core`, `flyway-database-postgresql`) — déjà dans la stack demandée ;
  - Graphiques (remplace recharts) : **Chart.js** (JS, vendu localement) — piloté par Alpine avec des données JSON rendues serveur ;
  - Markdown (remplace react-markdown) : **commonmark-java** — rendu serveur ;
  - PDF du bilan (remplace @react-pdf) : **openhtmltopdf** (HTML/CSS → PDF, gère SVG pour les courbes) ;
  - HTMX, Alpine.js, Basecoat UI, Tailwind : fichiers fournis comment ? cf. Q7.
- **Q7 — Build front.** Basecoat UI cible **Tailwind v4** (l'ancienne app est en v3, config JS). Je propose Tailwind v4 avec les tokens portés dans `@theme`, buildé via `frontend-maven-plugin` (Node local + npm : `tailwindcss`, `@tailwindcss/cli`, `basecoat-css`, `@tailwindcss/typography`, `tw-animate-css`, `htmx.org`, `alpinejs`, `chart.js`, `lucide-static`). Alternative : CLI Tailwind standalone + fichiers vendus dans le repo. Lequel préférez-vous ?
- **Q8 — Thème sombre.** Défini mais inactivable dans l'existant : je ne le porte pas, d'accord ?
- **Q9 — Premier owner.** Aujourd'hui attribué à la main en base. Le conserver (script SQL documenté), ou une propriété de démarrage (`app.bootstrap-owner-email`) ?
- **Q10 — Hors périmètre.** Je propose de ne pas porter : l'outil MCP Lovable, l'edge function `dispatch-announcement` (stub), les clés `page_title_*`, `apply_template`, `event_participants`. D'accord ?
- **Q11 — Langue des URL / package.** Je garde les URL existantes (`/app/coach/...`) et le package racine `fr.mtpilates`. D'accord ?
