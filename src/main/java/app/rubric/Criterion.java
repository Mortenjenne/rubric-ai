package app.rubric;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row of a Rubric: a single aspect of a Submission judged on its own,
 * traceable to the Source material it came from, with one descriptor per Level.
 */
@Entity
@Table(name = "criteria")
public class Criterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The stable business key from the bundled Rubric resource, e.g. "formkrav". */
    @Column(name = "criterion_key", nullable = false)
    private String key;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubric_version", nullable = false)
    private Rubric rubric;

    // A Criterion is always used together with its source material and Level descriptors, so
    // both collections below are loaded eagerly rather than left as lazy proxies.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "criterion_sources", joinColumns = @JoinColumn(name = "criterion_id"))
    @OrderColumn(name = "position")
    @Column(name = "source", nullable = false)
    private List<String> sourceMaterial;

    /** Level name (Mangelfuldt, Acceptabelt, Tilfredsstillende, Udmærket) to its descriptor. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "criterion_levels", joinColumns = @JoinColumn(name = "criterion_id"))
    @MapKeyColumn(name = "level_name")
    @Column(name = "descriptor", nullable = false, length = 2000)
    private Map<String, String> levels = new LinkedHashMap<>();

    protected Criterion() {
        // for Hibernate
    }

    public Criterion(String key, String name, int weight, String description,
                      List<String> sourceMaterial, Map<String, String> levels) {
        this.key = key;
        this.name = name;
        this.weight = weight;
        this.description = description;
        this.sourceMaterial = sourceMaterial;
        this.levels = levels;
    }

    void assignTo(Rubric rubric) {
        this.rubric = rubric;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public String getDescription() {
        return description;
    }

    public Rubric getRubric() {
        return rubric;
    }

    public List<String> getSourceMaterial() {
        return sourceMaterial;
    }

    public Map<String, String> getLevels() {
        return levels;
    }
}
