-- A published AssignmentVersion is frozen forever (ADR 0008): once an Evaluation records a
-- version number, an edit to that version's Rubric or Assessment stance would silently change
-- what an old Evaluation says it was judged against. The application never updates or deletes an
-- AssignmentVersion or its Criteria, but the schema enforces it too, so a future bug or a manual
-- `UPDATE` can't quietly break the guarantee.
CREATE FUNCTION reject_assignment_version_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'assignment_versions is immutable: % on assignment_versions is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_assignment_versions_immutable
    BEFORE UPDATE OR DELETE ON assignment_versions
    FOR EACH ROW EXECUTE FUNCTION reject_assignment_version_mutation();

-- A Criterion belongs to a Draft or to an AssignmentVersion, never both (chk_criteria_exactly_one_
-- parent), so the trigger below only ever fires for the version-owned half of the table.
-- Returns OLD/NEW (not NULL) for a draft-owned row, since a BEFORE-trigger returning NULL would
-- silently skip the row instead of letting the delete/update through.
--
-- An UPDATE that only rewrites `position` to the value it already had is let through rather than
-- rejected outright: Hibernate's persister for a List with @OrderColumn re-touches every sibling
-- criterion's position column whenever any other AssignmentVersion is added to the same Assignment
-- in the same flush, even though nothing about that sibling's content changed. What must never
-- change once published is the criterion's actual content and its ownership, so those columns are
-- compared explicitly; a real edit to any of them is still rejected.
CREATE FUNCTION reject_published_criterion_mutation() RETURNS trigger AS $$
BEGIN
    IF OLD.assignment_version_id IS NULL THEN
        IF TG_OP = 'DELETE' THEN
            RETURN OLD;
        ELSE
            RETURN NEW;
        END IF;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'criteria belonging to a published assignment_version are immutable: DELETE is not allowed';
    END IF;

    -- Every column but `position` is listed here on purpose: a future column added to `criteria`
    -- needs adding to this list too, or it would be mutable on a published row without this
    -- trigger noticing.
    IF NEW.criterion_key IS DISTINCT FROM OLD.criterion_key
        OR NEW.name IS DISTINCT FROM OLD.name
        OR NEW.weight IS DISTINCT FROM OLD.weight
        OR NEW.description IS DISTINCT FROM OLD.description
        OR NEW.draft_id IS DISTINCT FROM OLD.draft_id
        OR NEW.assignment_version_id IS DISTINCT FROM OLD.assignment_version_id THEN
        RAISE EXCEPTION 'criteria belonging to a published assignment_version are immutable: UPDATE of content is not allowed';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_published_criteria_immutable
    BEFORE UPDATE OR DELETE ON criteria
    FOR EACH ROW EXECUTE FUNCTION reject_published_criterion_mutation();
