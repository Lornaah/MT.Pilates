-- Inscription en deux temps (décision Q3) : le compte n'existe qu'après confirmation de l'email.
CREATE TABLE pending_registrations (
  id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email         text NOT NULL,
  password_hash text NOT NULL,
  token_hash    text NOT NULL UNIQUE,
  expires_at    timestamptz NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX pending_registrations_email_lower_uk ON pending_registrations (lower(email));
CREATE INDEX pending_registrations_expires_idx ON pending_registrations (expires_at);

-- Sessions persistantes (« remember-me » de Spring Security) : l'original gardait la session
-- Supabase dans le navigateur, l'utilisateur restait connecté entre deux visites.
CREATE TABLE persistent_logins (
  username  varchar(255) NOT NULL,
  series    varchar(64) PRIMARY KEY,
  token     varchar(64) NOT NULL,
  last_used timestamp NOT NULL
);
