package dev.liftoff.platform.project.service;

import dev.liftoff.platform.auth.entity.User;
import dev.liftoff.platform.auth.repository.UserRepository;
import dev.liftoff.platform.build.dto.BuildResponse;
import dev.liftoff.platform.common.exception.LiftoffException;
import dev.liftoff.platform.project.dto.ProjectCreateRequest;
import dev.liftoff.platform.project.dto.ProjectResponse;
import dev.liftoff.platform.project.entity.Build;
import dev.liftoff.platform.project.entity.Project;
import dev.liftoff.platform.project.repository.BuildRepository;
import dev.liftoff.platform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final BuildRepository buildRepository;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request, UUID ownerId) {
        if (projectRepository.existsBySubdomain(request.subdomain())) {
            throw new LiftoffException("Subdomain is already taken");
        }

        String slug = request.name().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        // Ensure slug uniqueness (simple implementation)
        if (projectRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 5);
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new LiftoffException("User not found"));

        Project project = new Project();
        project.setOwner(owner);
        project.setName(request.name());
        project.setSlug(slug);
        project.setGithubRepoUrl(request.githubRepoUrl());
        project.setBranch(request.branch());
        project.setSubdomain(request.subdomain());
        project.setGithubWebhookSecret(generateWebhookSecret());

        project = projectRepository.save(project);

        Build build = new Build();
        build.setProject(project);
        build.setBranch(project.getBranch());
        build.setStatus("QUEUED");
        buildRepository.save(build);

        return ProjectResponse.fromEntity(project);
    }

    public List<ProjectResponse> getUserProjects(UUID ownerId) {
        return projectRepository.findAllByOwnerId(ownerId).stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProject(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new LiftoffException("Project not found"));
        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public void triggerManualBuild(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new LiftoffException("Project not found"));

        Build build = new Build();
        build.setProject(project);
        build.setBranch(project.getBranch());
        build.setStatus("QUEUED");
        buildRepository.save(build);
    }

    public List<BuildResponse> getProjectBuilds(UUID projectId, UUID ownerId) {
        Project project = projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new LiftoffException("Project not found"));
        return buildRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(BuildResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private String generateWebhookSecret() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
