package app.educator;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EducatorRepository extends JpaRepository<Educator, UUID> {

    Optional<Educator> findByEmail(String email);

    boolean existsByEmail(String email);
}
