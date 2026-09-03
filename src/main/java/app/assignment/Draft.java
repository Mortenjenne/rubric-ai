package app.assignment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The one mutable surface of an Assignment: the Rubric and Assessment stance an Educator is
 * still tuning. Created together with its Assignment and never removed. Saving a Draft never
 * validates it — a half-written Rubric must stay saveable; validation belongs to publishing.
 */
@Entity
@Table(name = "drafts")
public class Draft {

    @Id
    private UUID id;

    @Column(name = "assessment_stance", nullable = false)
    private String assessmentStance;

    // A Draft is a small aggregate always used whole, so its Criteria are loaded eagerly rather
    // than left as a lazy proxy outside a session.
    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    private List<Criterion> criteria = new ArrayList<>();

    protected Draft() {
        // for Hibernate
    }

    Draft(UUID id) {
        this.id = id;
        this.assessmentStance = "";
    }

    void addCriterion(Criterion criterion) {
        criterion.assignToDraft(this);
        criteria.add(criterion);
    }

    public UUID getId() {
        return id;
    }

    public String getAssessmentStance() {
        return assessmentStance;
    }

    public List<Criterion> getCriteria() {
        return List.copyOf(criteria);
    }

    public Rubric getRubric() {
        return new Rubric(criteria);
    }
}
