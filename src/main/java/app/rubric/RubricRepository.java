package app.rubric;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RubricRepository extends JpaRepository<Rubric, Integer> {

    /**
     * The Rubric an Evaluation is judged against. Multiple Assignments and Rubric versions
     * are out of scope today — one Rubric ships, and "active" means the highest version seeded.
     */
    Optional<Rubric> findFirstByOrderByVersionDesc();
}
