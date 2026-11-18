-- Bilans PDF (M5) : brouillon modifiable par le coach, puis envoi (PDF figé, email, espace membre).
ALTER TABLE member_reports
  ADD COLUMN status   text NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'sent')),
  ADD COLUMN content  jsonb NOT NULL DEFAULT '{}'::jsonb,
  ADD COLUMN pdf_path text,
  ADD COLUMN sent_at  timestamptz;
