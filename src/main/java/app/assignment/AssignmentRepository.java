package app.assignment;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Extends the bare {@link Repository} marker rather than {@code JpaRepository}, and declares
 * only educator-scoped finders plus {@code save} — so there is no inherited {@code findById} or
 * {@code findAll} able to return an Assignment belonging to another Educator.
 */
public interface AssignmentRepository extends Repository<Assignment, UUID> {

    Assignment save(Assignment assignment);

    Optional<Assignment> findByIdAndEducatorId(UUID id, UUID educatorId);

    /** The Educator's own Assignments, excluding soft-deleted ones, most recently edited first. */
    List<Assignment> findAllByEducatorIdAndDeletedFalseOrderByUpdatedAtDesc(UUID educatorId);

    /**
     * The Educator's most recently created Assignment. A temporary internal seam for
     * {@link app.evaluation.service.EvaluationService} until ticket 09 adds {@code assignmentId}
     * to the evaluation request; not used by any public, multi-Assignment-aware endpoint.
     */
    Optional<Assignment> findFirstByEducatorIdOrderByCreatedAtDesc(UUID educatorId);
}
