-- An AssignmentVersion freezes everything the model is shown (ADR 0008), which includes the
-- Assignment's title at the moment it was published — publishing copies it in alongside the
-- Rubric and the Assessment stance, so a later rename doesn't change what an old Evaluation says
-- it was judged against.
ALTER TABLE assignment_versions ADD COLUMN title character varying(255) NOT NULL DEFAULT '';
ALTER TABLE assignment_versions ALTER COLUMN title DROP DEFAULT;
