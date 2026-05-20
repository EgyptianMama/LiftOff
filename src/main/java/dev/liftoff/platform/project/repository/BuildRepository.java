package dev.liftoff.platform.project.repository;

import dev.liftoff.platform.project.entity.Build;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildRepository extends JpaRepository<Build, UUID> {
    List<Build> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Build> findByGithubDeliveryId(String githubDeliveryId);
}
