package app.evaluation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * A judgement of one Submission against one Rubric version. Storage is hybrid, per the spec:
 * the queryable facts — identifier, Rubric version, provider, model, Suggested grade and
 * creation timestamp — are columns, while the narrative content (the overall assessment, the
 * Findings and the dialogue questions) is a single JSON document. The Submission text itself
 * is never a field here, per ADR 0003.
 */
@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    private UUID id;

    @Column(name = "rubric_version", nullable = false)
    private Integer rubricVersion;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(name = "suggested_grade", nullable = false)
    private String suggestedGrade;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private EvaluationDocument document;

    protected Evaluation() {
        // for Hibernate
    }

    public Evaluation(UUID id, Integer rubricVersion, String provider, String model,
                       String suggestedGrade, Instant createdAt, EvaluationDocument document) {
        this.id = id;
        this.rubricVersion = rubricVersion;
        this.provider = provider;
        this.model = model;
        this.suggestedGrade = suggestedGrade;
        this.createdAt = createdAt;
        this.document = document;
    }

    public UUID getId() {
        return id;
    }

    public Integer getRubricVersion() {
        return rubricVersion;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getSuggestedGrade() {
        return suggestedGrade;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public EvaluationDocument getDocument() {
        return document;
    }
}
