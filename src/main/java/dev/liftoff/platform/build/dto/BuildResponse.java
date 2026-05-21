package dev.liftoff.platform.build.dto;

import dev.liftoff.platform.project.entity.Build;
import java.time.Instant;
import java.util.UUID;

public record BuildResponse(
        UUID id,
        String status,
        String commitSha,
        String commitMessage,
        String branch,
        String frameworkDetected,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {
    public static BuildResponse fromEntity(Build build) {
        return new BuildResponse(
                build.getId(),
                build.getStatus(),
                build.getCommitSha(),
                build.getCommitMessage(),
                build.getBranch(),
                build.getFrameworkDetected(),
                build.getStartedAt(),
                build.getFinishedAt(),
                build.getCreatedAt()
        );
    }
}
