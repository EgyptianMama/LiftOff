package dev.liftoff.platform.project.dto;

import dev.liftoff.platform.project.entity.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String slug,
        String githubRepoUrl,
        String githubWebhookSecret,
        String branch,
        String subdomain,
        String customDomain,
        Instant createdAt
) {
    public static ProjectResponse fromEntity(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getSlug(),
                project.getGithubRepoUrl(),
                project.getGithubWebhookSecret(),
                project.getBranch(),
                project.getSubdomain(),
                project.getCustomDomain(),
                project.getCreatedAt()
        );
    }
}
