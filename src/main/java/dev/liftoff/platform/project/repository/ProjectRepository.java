package dev.liftoff.platform.project.repository;

import dev.liftoff.platform.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsBySlug(String slug);
    boolean existsBySubdomain(String subdomain);
    List<Project> findAllByOwnerId(UUID ownerId);
    Optional<Project> findByIdAndOwnerId(UUID id, UUID ownerId);
}
