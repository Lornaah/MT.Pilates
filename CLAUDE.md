# CLAUDE.md — M.T.Pilates (réécriture Spring Boot)

## Objectif
Réécrire l'application de coaching M.T.Pilates (ancienne stack : Vite + React + Supabase + shadcn/ui)
en application **Spring Boot server-side**. Le résultat doit être **fonctionnellement et visuellement
identique** à l'existant.

## Sources de vérité (ancien repo, en lecture seule)
Ancien code extrait dans `../legacy/mt.pilate.app-main/`.
- **Fonctionnalités, rôles, écrans, parcours** : `src/` (pages, composants, hooks, services).
- **Modèle de données** : `supabase/migrations/*.sql`, `src/integrations/supabase/types.ts` et les policies RLS.
- **Design** : `tailwind.config.ts`, `src/index.css` (variables CSS shadcn), `src/components/ui/*` (variantes réellement utilisées).
- L'inventaire de référence est `INVENTORY.md` ; le découpage en modules est `PLAN.md`.
- Les décisions validées (`INVENTORY.md` §9) priment sur le comportement de l'original :
  règles d'accès de la base, correction des failles et des bugs bloquants, inscription par vérification d'email.

**N'inventer aucune fonctionnalité.** En cas d'ambiguïté : la lister et poser la question avant de coder.

## Stack
- Java 21, Spring Boot 3.x, Maven (wrapper `./mvnw`)
- Spring Security : authentification par **session** + **form login**. Les policies RLS deviennent des
  règles d'autorisation côté service (`@PreAuthorize` ou vérifications explicites dans un `*AccessPolicy`).
- Spring Data JPA, PostgreSQL, migrations **Flyway** (`src/main/resources/db/migration`)
- Thymeleaf (layouts et fragments réutilisables), **HTMX** pour les interactions serveur,
  **Alpine.js** pour l'état purement client (dialogs, dropdowns, tabs, toasts)
- Tailwind CSS buildé via Maven ; **Basecoat UI** pour reproduire les composants shadcn en HTML
- Stockage de fichiers : abstraction `StorageService` (implémentation locale d'abord)
- Tests : JUnit 5, AssertJ, Spring Boot Test, Testcontainers PostgreSQL

**Pas de nouvelle dépendance sans la proposer d'abord** (Maven ou npm/CDN).

## Architecture
- **Package par feature** : `fr.mtpilates.<feature>` (ex. `auth`, `user`, `member`, `training`,
  `nutrition`, `calendar`, `announcement`, `document`, `habit`, `prospect`, `finance`, `config`…).
  Code transverse dans `fr.mtpilates.shared` (web, security, storage, ui).
- Dans chaque feature : `XxxController` → `XxxService` → `XxxRepository`.
- Les **entités JPA ne sortent jamais du service** : les controllers et les vues ne manipulent que des
  DTO / `record` (`XxxView`, `XxxForm`, `XxxSummary`…).
- Les règles d'accès (ex-RLS) vivent dans le service ou dans un composant `XxxAccessPolicy`
  dédié, testé unitairement. Ne jamais se fier uniquement au masquage dans le template.

## Conventions web
- Un controller renvoie **soit une page complète, soit un fragment** si la requête vient d'HTMX
  (header `HX-Request`). Utiliser l'utilitaire partagé (`HtmxRequest`) plutôt que de tester le header à la main.
- Validation avec **Bean Validation** (`@Valid` + `BindingResult`). En cas d'erreur, le formulaire est
  **re-rendu en fragment HTMX** avec les messages en français.
- Toasts : déclenchés par le serveur via le header `HX-Trigger` (`{"toast": {...}}`) et affichés par Alpine.
- Les routes reprennent celles de l'ancienne app (`/auth`, `/onboarding`, `/app/...`), redirections de
  compatibilité incluses.
- CSRF actif (token injecté dans les requêtes HTMX via `hx-headers` sur le `<body>`).

## Conventions Java
- **Pas de retour `null`** : utiliser `Optional`.
- **Pas de `catch (Exception e)`** : catcher des types précis.
- **Modificateurs d'accès toujours explicites** (y compris `private` sur les champs, `public` sur les méthodes d'interface implémentées).
- **Méthodes courtes** à responsabilité unique.
- **Commentaires uniquement pour expliquer le POURQUOI**, jamais le quoi.
- Injection par constructeur, champs `final`.
- Records pour DTO, formulaires et vues ; enums Java pour les enums Postgres et les CHECK.

## Conventions front
- **Aucun JavaScript métier hors Alpine.** Pas de SPA, pas de React.
- Design tokens : reprendre **à l'identique** les variables HSL de `src/index.css` et l'extension
  Tailwind de `tailwind.config.ts` (couleurs `sage`, `coach`, `setter`, `trial`, `bordeaux`, `success`,
  ombres `soft/warm/elevated`, dégradés, radius `1rem`, polices Fraunces/Inter, animations).
- Les variantes custom des composants (Button `hero/outlineLight/cream/sage`, Badge
  `success/warm/pop/coach/sage/bordeaux`, `rounded-full` sur les boutons…) sont redéfinies par-dessus Basecoat.
- Couleurs : toujours via les tokens (`bg-primary`, `text-muted-foreground`…). Les couleurs en dur de l'original
  (palette coach, semaines, types de repas, covers…) sont déclarées une fois en variables CSS (`--week-1`, `--meal-breakfast`…), jamais dans les templates.
- Un fragment Thymeleaf par composant réutilisable (`templates/fragments/ui/*.html`).
- Libellés et textes en français, repris mot pour mot de l'ancien code.

## Portage des classes Tailwind v3 → v4
L'original est en Tailwind v3, la cible en v4. En recopiant une classe, appliquer :
- `shadow-sm` → `shadow-xs`, `shadow` → `shadow-sm`, `outline-none` → `outline-hidden`, `rounded` → `rounded-sm`.
- Interligne : en v3, une taille de texte **responsive** (`md:text-xl`) écrase un `leading-*` non responsive ;
  en v4 c'est l'inverse. Ajouter le `leading` équivalent sur le même palier (ex. `md:leading-7` pour text-xl,
  `md:leading-none` pour text-5xl et plus). Vérifier par comparaison de captures.
- `container`, `hover:` (actif aussi sur écran tactile) et le curseur des boutons sont déjà alignés sur v3 dans `app.css`.
- Rayons : `rounded-lg/md/sm` suivent `--radius` (1rem) comme dans l'original.
- `space-x/y-*` sont redéfinis dans `app.css` avec la mécanique v3 (marge haute sur les enfants suivants).
- Icônes dans un bouton : toujours 16 px, comme l'original (`[&_svg]:size-4` battait toute classe de taille).
- `twMerge` de l'original : une classe `text-*` passée à un composant **supprimait** son `leading-*`.
  Ex. `<Label className="text-sm">` → interligne de `text-sm`, pas `leading-none` : ajouter `leading-5`.
- Basecoat diverge de shadcn sur `.label` (flex) et `.tabs` (flex) : le style pack les remet en inline / bloc.

## Pièges Thymeleaf / HTMX rencontrés
- Un littéral passé à un fragment ne peut pas contenir d'apostrophe : passer par `messages.properties` (`#{cle}`).
- `th:replace` / `th:insert` passent avant `th:if` sur le même élément : envelopper dans `<th:block th:if>`.
- Pas de lambda ni de `@bean` dans les expressions : préparer la valeur côté Java (modèle, `@ControllerAdvice`).
- En-têtes HTMX (`HX-Trigger`) : ASCII uniquement, `ToastResponses` échappe déjà le JSON.

## Thèmes
- Le thème par défaut est défini dans `src/main/frontend/css/themes/default.css` (variables HSL sur `:root`).
- Un thème supplémentaire (sombre, Noël…) est un fichier `themes/<nom>.css` qui redéfinit les variables sous
  `[data-theme="<nom>"]`, avec d'éventuels décors (images, `::before`) limités à ce fichier.
- Le thème actif est l'attribut `data-theme` posé sur `<html>` par le layout. Ajouter un thème ne doit demander
  **aucune modification de template**. Le mécanisme de choix du thème (préférence, date, config) n'existe pas
  dans l'original : il sera ajouté seulement sur demande.
- Les tokens `.dark` de l'original sont portés dans `themes/dark.css`, non activés.

## Comptes et configuration sensibles
- Aucun email, mot de passe ou secret dans le code ni dans les migrations.
- Owners amorcés par `APP_BOOTSTRAP_OWNER_EMAILS` (liste séparée par des virgules), lue au démarrage.
- SMTP configuré par variables d'environnement (`SPRING_MAIL_*`).

## Commandes
```bash
docker compose up -d                # PostgreSQL + Mailpit (emails capturés : http://localhost:8025)
set -a; . ./.env; set +a            # variables locales (owners, SMTP), fichier non commité
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # lancement local (http://localhost:8080)
./mvnw clean verify                 # build complet : Tailwind + compilation + tests (Testcontainers)
./mvnw test                         # tests unitaires et d'intégration
./mvnw -Dtest=XxxTest test          # un seul test
```
Les tests d'intégration nécessitent un démon Docker (Testcontainers).
En développement : `npm run watch:css` recompile le CSS à chaud ; le profil `dev` relit templates et CSS sans redémarrage.
Catalogue des composants (profil dev) : http://localhost:8080/dev/ui (`?theme=dark` pour tester un thème).

## Démarche par module
1. Implémenter le module **avec ses tests** (service + autorisations + controller/MockMvc).
2. Vérifier que `./mvnw clean verify` passe.
3. **Comparer visuellement** chaque écran avec l'original (captures Playwright côte à côte).
4. Commit, puis passer au module suivant **uniquement sur demande**.
