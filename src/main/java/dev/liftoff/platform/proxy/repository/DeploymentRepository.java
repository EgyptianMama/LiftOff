package dev.liftoff.platform.proxy.repository;

import dev.liftoff.platform.proxy.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, UUID> {
    
    @Modifying
    @Query("UPDATE Deployment d SET d.isLive = false WHERE d.project.id = :projectId AND d.isLive = true")
    void markAllDeploymentsNotLiveForProject(UUID projectId);
    
    Optional<Deployment> findByProjectIdAndIsLiveTrue(UUID projectId);
}
