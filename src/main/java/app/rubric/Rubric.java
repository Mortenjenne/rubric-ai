package app.rubric;

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

/**
 * One version of the assessment matrix a Submission is judged against.
 * A seeded version is immutable: later Evaluations must keep referring to the
 * Rubric that actually judged them, so rows are inserted once and never updated.
 */
@Entity
@Table(name = "rubrics")
public class Rubric {

    @Id
    private Integer version;

    @Column(nullable = false)
    private String assignment;

    @Column(nullable = false)
    private String language;

    @Column(length = 2000)
    private String note;

    // The Rubric is a small aggregate always used whole (six Criteria), so it is loaded eagerly
    // rather than leaving callers to trip over a lazy proxy outside a session.
    @OneToMany(mappedBy = "rubric", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    private List<Criterion> criteria = new ArrayList<>();

    protected Rubric() {
        // for Hibernate
    }

    public Rubric(Integer version, String assignment, String language, String note) {
        this.version = version;
        this.assignment = assignment;
        this.language = language;
        this.note = note;
    }

    public void addCriterion(Criterion criterion) {
        criterion.assignTo(this);
        criteria.add(criterion);
    }

    public Integer getVersion() {
        return version;
    }

    public String getAssignment() {
        return assignment;
    }

    public String getLanguage() {
        return language;
    }

    public String getNote() {
        return note;
    }

    public List<Criterion> getCriteria() {
        return criteria;
    }
}
