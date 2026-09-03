package app.assignment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A frozen snapshot of everything the model is shown: the Rubric and the Assessment stance, and
 * (once Source material lands) the Source material. Numbered per Assignment, starting at 1. No
 * mapping, service or endpoint updates or deletes a persisted version — it carries no setters
 * after construction, and nothing here ever calls one on its Criteria either.
 */
@Entity
@Table(name = "assignment_versions")
public class AssignmentVersion {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "assessment_stance", nullable = false)
    private String assessmentStance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // An AssignmentVersion is a small aggregate always used whole, so its Criteria are loaded
    // eagerly rather than left as a lazy proxy outside a session. Cascade is ALL, not just
    // PERSIST, because save() on a manually-assigned id merges rather than persists, and merge
    // only cascades through MERGE/ALL — see the matching note on Assignment.versions.
    @OneToMany(mappedBy = "assignmentVersion", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    private List<Criterion> criteria = new ArrayList<>();

    protected AssignmentVersion() {
        // for Hibernate
    }

    AssignmentVersion(UUID id, int versionNumber, String assessmentStance, Instant createdAt) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.assessmentStance = assessmentStance;
        this.createdAt = createdAt;
    }

    void assignTo(Assignment assignment) {
        this.assignment = assignment;
    }

    void addCriterion(Criterion criterion) {
        criterion.assignToVersion(this);
        criteria.add(criterion);
    }

    public UUID getId() {
        return id;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getAssessmentStance() {
        return assessmentStance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Criterion> getCriteria() {
        return List.copyOf(criteria);
    }

    public Rubric getRubric() {
        return new Rubric(criteria);
    }
}
