package app.assignment;

import app.educator.Educator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owned by exactly one Educator: a title, a soft-delete flag, exactly one mutable Draft created
 * with it and never removed, and any number of frozen, published AssignmentVersions numbered
 * from 1. Criterion keys are assigned sequentially ({@code c1}, {@code c2}, …) by this aggregate
 * whenever a Criterion is added without an explicit key of its own — never derived from the
 * Criterion's name, and never reused, so a rename, a reorder or a later deletion cannot orphan
 * the Findings of an Evaluation that already ran against an earlier key.
 */
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educator_id", nullable = false)
    private Educator educator;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "next_criterion_sequence", nullable = false)
    private int nextCriterionSequence = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // An Assignment is a small aggregate always used whole, so its Draft and versions are loaded
    // eagerly rather than left as lazy proxies outside a session.
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "draft_id", nullable = false, unique = true)
    private Draft draft;

    // save() on a manually-assigned id triggers a merge, not a persist, and JPA merge only
    // cascades through associations marked MERGE (or ALL) — PERSIST alone would leave a brand
    // new version's rows unreachable from the cascade and make Hibernate look them up as if they
    // already existed. Nothing here ever mutates a persisted version once merged in, so ALL is
    // safe despite the immutability this aggregate otherwise enforces by never exposing setters.
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("versionNumber ASC")
    private List<AssignmentVersion> versions = new ArrayList<>();

    protected Assignment() {
        // for Hibernate
    }

    public Assignment(UUID id, Educator educator, String title, Instant createdAt) {
        this.id = id;
        this.educator = educator;
        this.title = title;
        this.createdAt = createdAt;
        this.draft = new Draft(UUID.randomUUID());
    }

    /** Adds a Criterion to the Draft with an explicit, caller-chosen key — how a Template's
     * readable keys (e.g. "formkrav") survive being copied into a new Assignment. */
    public Criterion addDraftCriterion(String key, String name, int weight, String description,
                                        List<String> sourceReferences, Map<String, String> levels) {
        Criterion criterion = new Criterion(key, name, weight, description, sourceReferences, levels);
        draft.addCriterion(criterion);
        return criterion;
    }

    /** Adds a Criterion to the Draft with the aggregate's next sequential key. */
    public Criterion addDraftCriterion(String name, int weight, String description,
                                        List<String> sourceReferences, Map<String, String> levels) {
        return addDraftCriterion(nextCriterionKey(), name, weight, description, sourceReferences, levels);
    }

    /** Replaces the Draft's Assessment stance — how a Template's stance is copied in when the
     * Assignment is created, since a fresh Draft otherwise starts blank. */
    public void setDraftAssessmentStance(String assessmentStance) {
        draft.replaceAssessmentStance(assessmentStance);
    }

    private String nextCriterionKey() {
        return "c" + nextCriterionSequence++;
    }

    /** Snapshots the Draft's Rubric and Assessment stance into a new, frozen AssignmentVersion,
     * numbered one higher than the current highest. Leaves the Draft unchanged, so the
     * Educator's next edit continues from where they were. Carries no validation of its own —
     * that gate belongs to the publish endpoint, not the aggregate. */
    public AssignmentVersion publishVersion(Instant createdAt) {
        int versionNumber = versions.stream()
                .mapToInt(AssignmentVersion::getVersionNumber)
                .max()
                .orElse(0) + 1;

        AssignmentVersion version = new AssignmentVersion(
                UUID.randomUUID(), versionNumber, draft.getAssessmentStance(), createdAt);
        version.assignTo(this);
        for (Criterion criterion : draft.getCriteria()) {
            version.addCriterion(new Criterion(
                    criterion.getKey(), criterion.getName(), criterion.getWeight(), criterion.getDescription(),
                    List.copyOf(criterion.getSourceReferences()), Map.copyOf(criterion.getLevels())));
        }
        versions.add(version);
        return version;
    }

    public Optional<AssignmentVersion> latestVersion() {
        return versions.stream().max(Comparator.comparingInt(AssignmentVersion::getVersionNumber));
    }

    public UUID getId() {
        return id;
    }

    public Educator getEducator() {
        return educator;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Draft getDraft() {
        return draft;
    }

    public List<AssignmentVersion> getVersions() {
        return List.copyOf(versions);
    }
}
