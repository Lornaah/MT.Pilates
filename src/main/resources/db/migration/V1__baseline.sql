-- =============================================================================
-- Schéma consolidé de M.T.Pilates : état final des 38 migrations Supabase.
-- Différences voulues avec l'original :
--   * `auth.users` (Supabase) est remplacée par la table applicative `users` ;
--   * les enums Postgres deviennent du texte + CHECK (même ensemble de valeurs,
--     plus simple à faire évoluer et à mapper en JPA) ;
--   * les colonnes « propriétaire » reçoivent une clé étrangère vers `users`
--     pour garantir l'intégrité (l'original s'appuyait sur auth.users) ;
--   * les règles RLS ne sont pas reproduites en base : elles sont appliquées
--     dans les services (voir INVENTORY.md §4) ;
--   * non portés (INVENTORY.md §9, Q10) : event_participants, apply_template,
--     v_announcements_for_user (remplacée par une requête de repository).
-- =============================================================================

CREATE OR REPLACE FUNCTION touch_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

-- ---------------------------------------------------------------- utilisateurs
CREATE TABLE users (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email         text NOT NULL,
  password_hash text NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_email_lower_uk ON users (lower(email));
CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE profiles (
  id                      uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  email                   text NOT NULL,
  first_name              text,
  last_name               text,
  phone                   text,
  date_of_birth           date,
  avatar_url              text,
  bio                     text,
  goals                   text,
  height_cm               numeric,
  weight_kg               numeric,
  measurements            jsonb DEFAULT '{}'::jsonb,
  weekly_target_sessions  integer DEFAULT 2,
  coach_notes             text,
  status                  text DEFAULT 'active'
                            CHECK (status IN ('active','paused','archived','finished')),
  subscription_start_date date,
  subscription_end_date   date,
  coach_id                uuid REFERENCES users(id) ON DELETE SET NULL,
  allergies               text[] NOT NULL DEFAULT '{}',
  pathologies             text[] NOT NULL DEFAULT '{}',
  work_sedentarity        text CHECK (work_sedentarity IS NULL OR work_sedentarity IN ('sedentary','light_activity','active','very_active')),
  sport_history           text,
  body_type               text CHECK (body_type IS NULL OR body_type IN ('ectomorph','endomorph','mesomorph','mixed')),
  coaching_motivation     text,
  external_summary        text,
  dietary_preferences     jsonb NOT NULL DEFAULT '{}'::jsonb,
  onboarding_completed_at timestamptz,
  terms_accepted_at       timestamptz,
  objectives              text[] NOT NULL DEFAULT '{}',
  available_equipment     text[] NOT NULL DEFAULT '{}',
  availability            jsonb NOT NULL DEFAULT '{}'::jsonb,
  billing_source          text,
  contract_amount         numeric(10,2) NOT NULL DEFAULT 0,
  paid_amount             numeric(10,2) NOT NULL DEFAULT 0,
  payment_installments    integer NOT NULL DEFAULT 1,
  billing_notes           text,
  created_at              timestamptz NOT NULL DEFAULT now(),
  updated_at              timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_profiles_coach ON profiles(coach_id);
CREATE INDEX idx_profiles_objectives ON profiles USING gin (objectives);
CREATE INDEX idx_profiles_available_equipment ON profiles USING gin (available_equipment);
CREATE TRIGGER trg_profiles_updated BEFORE UPDATE ON profiles
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE user_roles (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role       text NOT NULL CHECK (role IN ('owner','admin','coach','user','setter','member','external_member',
                                           'manager_coach','manager_setter','manager_closer','closer')),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, role)
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);

-- ------------------------------------------------------------- configuration
CREATE TABLE app_config (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  key        text NOT NULL UNIQUE,
  value      text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_app_config_updated BEFORE UPDATE ON app_config
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- Valeurs semées par l'original : elles priment sur les libellés par défaut du code.
INSERT INTO app_config (key, value) VALUES
  ('prospect_col_new', 'Nouveaux'),
  ('prospect_col_to_call', 'À rappeler'),
  ('prospect_col_trial', 'En essai'),
  ('prospect_col_converted', 'Convertis'),
  ('prospect_status_new', 'Nouveau'),
  ('prospect_status_to_call', 'À rappeler'),
  ('prospect_status_trial', 'En essai'),
  ('prospect_status_converted', 'Converti'),
  ('prospect_status_lost', 'Perdu'),
  ('nav_dashboard', 'Tableau de bord'),
  ('nav_calendar', 'Mon calendrier'),
  ('nav_members', 'Mes coachés'),
  ('nav_videos', 'Vidéos'),
  ('nav_admin_members', 'Membres'),
  ('nav_admin_prospects', 'Prospects'),
  ('nav_admin_finance', 'Finance'),
  ('nav_owner_roles', 'Rôles & équipe'),
  ('nav_member_program', 'Mon programme'),
  ('nav_member_videos', 'Bibliothèque'),
  ('nav_member_nutrition', 'Nutrition'),
  ('nav_member_profile', 'Ma fiche'),
  ('onboarding_video_url', ''),
  ('program_objectives', 'Endométriose,Règles douloureuses,Handicap,Perte de poids,Prise de masse,Renforcement,Mobilité,Pré/post-natal,Bien-être');

-- -------------------------------------------------------------------- vidéos
CREATE TABLE videos (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title            text NOT NULL,
  description      text,
  youtube_id       text NOT NULL,
  thumbnail_url    text,
  duration_minutes integer,
  level            text DEFAULT 'beginner' CHECK (level IN ('beginner','intermediate','advanced')),
  focus_areas      text[] DEFAULT '{}',
  tags             text[] DEFAULT '{}',
  created_by       uuid REFERENCES users(id) ON DELETE SET NULL,
  is_published     boolean DEFAULT true,
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_videos_updated BEFORE UPDATE ON videos
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE video_views (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  video_id   uuid NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
  watched_at timestamptz NOT NULL DEFAULT now(),
  completed  boolean DEFAULT false,
  rating     integer CHECK (rating BETWEEN 1 AND 5),
  notes      text
);
CREATE INDEX idx_video_views_user ON video_views(user_id);

-- --------------------------------------------------------------- calendrier
CREATE TABLE events (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title       text NOT NULL,
  description text,
  type        text NOT NULL DEFAULT 'live_class'
                CHECK (type IN ('live_class','studio_class','one_on_one','call','admin_task')),
  starts_at   timestamptz NOT NULL,
  ends_at     timestamptz NOT NULL,
  capacity    integer,
  location    text,
  meeting_url text,
  coach_id    uuid REFERENCES users(id) ON DELETE SET NULL,
  member_id   uuid REFERENCES users(id) ON DELETE SET NULL,
  is_private  boolean DEFAULT false,
  created_by  uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_starts ON events(starts_at);
CREATE INDEX idx_events_member ON events(member_id);
CREATE INDEX idx_events_coach ON events(coach_id);
CREATE TRIGGER trg_events_updated BEFORE UPDATE ON events
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ------------------------------------------------------------- suivi membre
CREATE TABLE member_tracking (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  date         date NOT NULL DEFAULT CURRENT_DATE,
  weight_kg    numeric,
  body_fat_pct numeric,
  waist_cm     numeric,
  hip_cm       numeric,
  chest_cm     numeric,
  arm_cm       numeric,
  thigh_cm     numeric,
  notes        text,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_member_tracking_user_date ON member_tracking(user_id, date DESC);
CREATE TRIGGER trg_member_tracking_updated BEFORE UPDATE ON member_tracking
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE member_photos (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  taken_at   date NOT NULL DEFAULT CURRENT_DATE,
  photo_url  text NOT NULL,
  side       text NOT NULL CHECK (side IN ('front','side','back')),
  notes      text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_member_photos_user_date ON member_photos(user_id, taken_at DESC);

CREATE TABLE member_documents (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  uploaded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
  title           text NOT NULL,
  category        text,
  file_url        text NOT NULL,
  file_size_bytes bigint,
  mime_type       text,
  uploaded_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_member_documents_user ON member_documents(user_id, uploaded_at DESC);

CREATE TABLE member_reports (
  id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                  uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  created_by               uuid REFERENCES profiles(id) ON DELETE SET NULL,
  period_start             date NOT NULL,
  period_end               date NOT NULL,
  target_weight_kg         numeric,
  start_weight_override_kg numeric,
  snapshot                 jsonb NOT NULL,
  notes                    text,
  created_at               timestamptz NOT NULL DEFAULT now(),
  updated_at               timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX member_reports_user_idx ON member_reports(user_id, period_end DESC);
CREATE TRIGGER trg_member_reports_updated BEFORE UPDATE ON member_reports
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE weekly_reviews (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  week_start_date  date NOT NULL,
  mental_wellbeing integer NOT NULL CHECK (mental_wellbeing BETWEEN 1 AND 10),
  free_note        text,
  created_at       timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, week_start_date)
);
CREATE INDEX idx_weekly_reviews_user ON weekly_reviews(user_id, week_start_date DESC);

-- ---------------------------------------------------------------- habitudes
CREATE TABLE habits (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title       text NOT NULL,
  description text,
  icon        text,
  frequency   text NOT NULL DEFAULT 'daily' CHECK (frequency IN ('daily','weekly','custom')),
  is_global   boolean NOT NULL DEFAULT false,
  created_by  uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at  timestamptz NOT NULL DEFAULT now(),
  updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_habits_updated BEFORE UPDATE ON habits
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE habit_assignments (
  id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  habit_id    uuid NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
  user_id     uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  assigned_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (habit_id, user_id)
);
CREATE INDEX idx_habit_assignments_user ON habit_assignments(user_id);

CREATE TABLE habit_completions (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  habit_id       uuid NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
  user_id        uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  completed_date date NOT NULL,
  created_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (habit_id, user_id, completed_date)
);
CREATE INDEX idx_habit_completions_user_date ON habit_completions(user_id, completed_date);

-- ---------------------------------------------------------------- annonces
CREATE TABLE announcements (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title           text NOT NULL,
  body            text NOT NULL,
  cover_image_url text,
  channel         text NOT NULL DEFAULT 'in_app' CHECK (channel IN ('in_app','email','push','all')),
  target          text NOT NULL DEFAULT 'all' CHECK (target IN ('all','active','specific','by_coach')),
  target_user_ids uuid[] NOT NULL DEFAULT '{}',
  target_coach_id uuid REFERENCES users(id) ON DELETE SET NULL,
  is_recurring    boolean NOT NULL DEFAULT false,
  recurrence_rule text,
  publish_at      timestamptz NOT NULL DEFAULT now(),
  created_by      uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_announcements_publish_at ON announcements(publish_at DESC);

CREATE TABLE announcement_reads (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  announcement_id uuid NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
  user_id         uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  read_at         timestamptz NOT NULL DEFAULT now(),
  UNIQUE (announcement_id, user_id)
);
CREATE INDEX idx_announcement_reads_user ON announcement_reads(user_id);

-- --------------------------------------------------------------- documents
CREATE TABLE documents (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title           text NOT NULL,
  description     text,
  category        text NOT NULL CHECK (category IN ('guide','fiche_pratique','contrat','recette','autre')),
  tags            text[] NOT NULL DEFAULT '{}',
  file_url        text NOT NULL,
  file_size_bytes integer,
  mime_type       text,
  visibility      text NOT NULL DEFAULT 'staff_only' CHECK (visibility IN ('staff_only','all_members','specific')),
  shared_with_ids uuid[] NOT NULL DEFAULT '{}',
  uploaded_by     uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_category ON documents(category);
CREATE INDEX idx_documents_visibility ON documents(visibility);
CREATE TRIGGER trg_documents_updated BEFORE UPDATE ON documents
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ------------------------------------------------------------- entraînement
CREATE TABLE exercises (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title              text NOT NULL,
  description        text,
  video_url          text,
  demo_url           text,
  thumbnail_url      text,
  muscle_groups      text[] NOT NULL DEFAULT '{}',
  equipment          text[] NOT NULL DEFAULT '{}',
  exercise_type      text,
  default_attributes jsonb NOT NULL DEFAULT '[]'::jsonb,
  objectives         text[] NOT NULL DEFAULT '{}',
  is_favorite        boolean NOT NULL DEFAULT false,
  created_by         uuid REFERENCES users(id) ON DELETE SET NULL,
  is_published       boolean NOT NULL DEFAULT true,
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_exercises_muscle_groups ON exercises USING gin (muscle_groups);
CREATE INDEX idx_exercises_equipment ON exercises USING gin (equipment);
CREATE INDEX idx_exercises_objectives ON exercises USING gin (objectives);
CREATE INDEX idx_exercises_type ON exercises (exercise_type);
CREATE TRIGGER trg_exercises_updated BEFORE UPDATE ON exercises
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE workout_sessions (
  id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title            text NOT NULL,
  description      text,
  category         text,
  duration_minutes integer,
  level            text CHECK (level IN ('beginner','intermediate','advanced')),
  is_template      boolean NOT NULL DEFAULT false,
  is_published     boolean NOT NULL DEFAULT true,
  created_by       uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at       timestamptz NOT NULL DEFAULT now(),
  updated_at       timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_workout_sessions_updated BEFORE UPDATE ON workout_sessions
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE workout_session_blocks (
  id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  position   integer NOT NULL,
  block_type text NOT NULL DEFAULT 'single' CHECK (block_type IN ('single','superset','triset','circuit')),
  rounds     integer NOT NULL DEFAULT 1,
  notes      text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (session_id, position)
);
CREATE INDEX idx_workout_session_blocks_session ON workout_session_blocks(session_id);
CREATE TRIGGER trg_workout_session_blocks_updated BEFORE UPDATE ON workout_session_blocks
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE workout_session_exercises (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  block_id          uuid NOT NULL REFERENCES workout_session_blocks(id) ON DELETE CASCADE,
  exercise_id       uuid NOT NULL REFERENCES exercises(id) ON DELETE RESTRICT,
  position_in_block integer NOT NULL DEFAULT 0,
  attributes        jsonb NOT NULL DEFAULT '[]'::jsonb,
  sets              integer NOT NULL DEFAULT 1,
  exercise_note     text,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  UNIQUE (block_id, position_in_block),
  CHECK (jsonb_typeof(attributes) = 'array' AND jsonb_array_length(attributes) <= 5)
);
CREATE INDEX idx_workout_session_exercises_block ON workout_session_exercises(block_id);
CREATE INDEX idx_workout_session_exercises_exercise ON workout_session_exercises(exercise_id);
CREATE TRIGGER trg_workout_session_exercises_updated BEFORE UPDATE ON workout_session_exercises
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- ---------------------------------------------------------------- nutrition
CREATE TABLE meal_plan_templates (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title               text NOT NULL,
  description         text,
  status              text NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','published','archived')),
  cover_color         text,
  kcal_target         integer,
  macros_target       jsonb,
  meals_per_day       integer NOT NULL DEFAULT 3 CHECK (meals_per_day BETWEEN 1 AND 8),
  dietary_preferences jsonb,
  duration_weeks      integer NOT NULL DEFAULT 1 CHECK (duration_weeks BETWEEN 1 AND 12),
  weeks               jsonb NOT NULL DEFAULT '[]'::jsonb,
  usage_count         integer NOT NULL DEFAULT 0,
  created_by          uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX meal_plan_templates_status_idx ON meal_plan_templates(status);
CREATE INDEX meal_plan_templates_created_by_idx ON meal_plan_templates(created_by);
CREATE TRIGGER trg_meal_plan_templates_updated BEFORE UPDATE ON meal_plan_templates
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE nutrition_plans (
  id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  member_id               uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  coach_id                uuid REFERENCES users(id) ON DELETE SET NULL,
  title                   text NOT NULL,
  description             text,
  meals                   jsonb NOT NULL DEFAULT '{}'::jsonb,
  starts_on               date,
  ends_on                 date,
  is_active               boolean NOT NULL DEFAULT true,
  shopping_list           jsonb NOT NULL DEFAULT '[]'::jsonb,
  pdf_url                 text,
  sent_at                 timestamptz,
  kcal_target             integer,
  macros_target           jsonb,
  generated_from_template boolean NOT NULL DEFAULT false,
  hide_macros_from_member boolean NOT NULL DEFAULT false,
  source_template_id      uuid REFERENCES meal_plan_templates(id) ON DELETE SET NULL,
  created_at              timestamptz NOT NULL DEFAULT now(),
  updated_at              timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_nutrition_member ON nutrition_plans(member_id);
CREATE INDEX nutrition_plans_source_template_id_idx ON nutrition_plans(source_template_id);
CREATE TRIGGER trg_nutrition_plans_updated BEFORE UPDATE ON nutrition_plans
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE recipes (
  id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title             text NOT NULL,
  description       text,
  category          text NOT NULL DEFAULT 'lunch',
  ingredients       jsonb NOT NULL DEFAULT '[]'::jsonb,
  instructions      text,
  calories          integer,
  protein_g         numeric,
  carbs_g           numeric,
  fat_g             numeric,
  source            text NOT NULL DEFAULT 'manual' CHECK (source IN ('manual','open_food_facts','user_imported')),
  external_id       text,
  external_url      text,
  image_url         text,
  video_url         text,
  prep_time_minutes integer,
  cook_time_minutes integer,
  servings          integer NOT NULL DEFAULT 1,
  dietary_tags      text[] NOT NULL DEFAULT '{}',
  allergens         text[] NOT NULL DEFAULT '{}',
  equipment_needed  text[] NOT NULL DEFAULT '{}',
  is_dessert        boolean NOT NULL DEFAULT false,
  meal_types        text[] NOT NULL DEFAULT '{}',
  created_by        uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_recipes_dietary_tags ON recipes USING gin (dietary_tags);
CREATE INDEX idx_recipes_allergens ON recipes USING gin (allergens);
CREATE INDEX idx_recipes_meal_types ON recipes USING gin (meal_types);
CREATE TRIGGER trg_recipes_updated BEFORE UPDATE ON recipes
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE foods (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name               text NOT NULL,
  brand              text,
  barcode            text,
  serving_size_g     numeric,
  calories_per_100g  numeric,
  protein_g_per_100g numeric,
  carbs_g_per_100g   numeric,
  fat_g_per_100g     numeric,
  fiber_g_per_100g   numeric,
  source             text NOT NULL DEFAULT 'manual' CHECK (source IN ('manual','open_food_facts','ciqual','custom_coach')),
  external_id        text,
  aisle              text CHECK (aisle IN ('fruits_legumes','viandes_poissons','laitages','epicerie_salee','epicerie_sucree',
                                           'boulangerie','surgeles','boissons','autre')),
  is_public          boolean NOT NULL DEFAULT false,
  created_by         uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_foods_name ON foods USING gin (to_tsvector('simple', name));
CREATE INDEX idx_foods_barcode ON foods (barcode) WHERE barcode IS NOT NULL;
CREATE INDEX idx_foods_source ON foods (source);
CREATE TRIGGER trg_foods_updated BEFORE UPDATE ON foods
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

-- -------------------------------------------------------------- programmes
CREATE TABLE programs (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title          text NOT NULL,
  description    text,
  status         text NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','scheduled','active','finished','template')),
  assignee_id    uuid REFERENCES profiles(id) ON DELETE SET NULL,
  start_date     date,
  duration_weeks integer,
  objectives     text[] NOT NULL DEFAULT '{}',
  created_by     uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_programs_assignee ON programs(assignee_id);
CREATE INDEX idx_programs_status ON programs(status);
CREATE INDEX idx_programs_objectives ON programs USING gin (objectives);
CREATE TRIGGER trg_programs_updated BEFORE UPDATE ON programs
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE program_weeks (
  id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  program_id   uuid NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
  week_number  integer NOT NULL,
  week_comment text,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  UNIQUE (program_id, week_number)
);
CREATE INDEX idx_program_weeks_program ON program_weeks(program_id);
CREATE TRIGGER trg_program_weeks_updated BEFORE UPDATE ON program_weeks
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE program_days (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  program_week_id    uuid NOT NULL REFERENCES program_weeks(id) ON DELETE CASCADE,
  day_of_week        integer NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
  day_type           text NOT NULL DEFAULT 'workout' CHECK (day_type IN ('workout','rest','review','nutrition','mixed')),
  day_comment        text,
  workout_session_id uuid REFERENCES workout_sessions(id) ON DELETE SET NULL,
  nutrition_plan_id  uuid REFERENCES nutrition_plans(id) ON DELETE SET NULL,
  created_at         timestamptz NOT NULL DEFAULT now(),
  updated_at         timestamptz NOT NULL DEFAULT now(),
  UNIQUE (program_week_id, day_of_week)
);
CREATE INDEX idx_program_days_week ON program_days(program_week_id);
CREATE TRIGGER trg_program_days_updated BEFORE UPDATE ON program_days
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();

CREATE TABLE program_day_sessions (
  id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  program_day_id     uuid NOT NULL REFERENCES program_days(id) ON DELETE CASCADE,
  workout_session_id uuid NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
  position           integer NOT NULL DEFAULT 0,
  created_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_pds_day ON program_day_sessions(program_day_id);

CREATE TABLE program_day_completions (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  program_day_id uuid NOT NULL REFERENCES program_days(id) ON DELETE CASCADE,
  user_id        uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  completed_at   timestamptz NOT NULL DEFAULT now(),
  member_notes   text,
  UNIQUE (program_day_id, user_id)
);
CREATE INDEX idx_program_day_completions_user ON program_day_completions(user_id);

-- ------------------------------------------------------------------- ventes
CREATE TABLE prospects (
  id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name          text NOT NULL,
  last_name           text,
  email               text,
  phone               text,
  age                 integer,
  source              text DEFAULT 'other'
                        CHECK (source IN ('instagram','tiktok','website','word_of_mouth','referral','other')),
  status              text NOT NULL DEFAULT 'new'
                        CHECK (status IN ('new','to_call','trial','converted','lost')),
  tag                 text,
  notes               text,
  objectives          text,
  next_followup_at    timestamptz,
  appointment_at      timestamptz,
  assigned_setter_id  uuid REFERENCES users(id) ON DELETE SET NULL,
  assigned_closer_id  uuid REFERENCES users(id) ON DELETE SET NULL,
  converted_member_id uuid REFERENCES profiles(id) ON DELETE SET NULL,
  created_by          uuid REFERENCES users(id) ON DELETE SET NULL,
  created_at          timestamptz NOT NULL DEFAULT now(),
  updated_at          timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_prospects_status ON prospects(status);
CREATE INDEX idx_prospects_setter ON prospects(assigned_setter_id);
CREATE TRIGGER trg_prospects_updated BEFORE UPDATE ON prospects
  FOR EACH ROW EXECUTE FUNCTION touch_updated_at();
