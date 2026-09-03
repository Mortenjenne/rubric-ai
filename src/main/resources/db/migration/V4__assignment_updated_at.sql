-- Tracks when an Assignment's Draft was last replaced, so the Assignment list can show "last
-- edited" without joining to the Draft. Backfilled to created_at for any existing row; every row
-- from here on is written by the application, which always sets it explicitly.
ALTER TABLE assignments ADD COLUMN updated_at timestamp(6) with time zone;
UPDATE assignments SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE assignments ALTER COLUMN updated_at SET NOT NULL;
