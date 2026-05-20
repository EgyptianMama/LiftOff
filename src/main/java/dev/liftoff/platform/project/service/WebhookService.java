package dev.liftoff.platform.project.service;

import dev.liftoff.platform.common.exception.LiftoffException;
import dev.liftoff.platform.project.dto.GithubWebhookPayload;
import dev.liftoff.platform.project.entity.Build;
import dev.liftoff.platform.project.entity.Project;
import dev.liftoff.platform.project.repository.BuildRepository;
import dev.liftoff.platform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ProjectRepository projectRepository;
    private final BuildRepository buildRepository;

    @Transactional
    public void processGithubPush(UUID projectId, GithubWebhookPayload payload, String githubDeliveryId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new LiftoffException("Project not found"));

        // Only process pushes to the configured branch
        String branchRef = "refs/heads/" + project.getBranch();
        if (!branchRef.equals(payload.ref())) {
            log.info("Ignoring push to {} for project {}. Expected {}", payload.ref(), projectId, branchRef);
            return;
        }

        // Idempotency: Check if we've already processed this exact webhook delivery
        if (githubDeliveryId != null && buildRepository.findByGithubDeliveryId(githubDeliveryId).isPresent()) {
            log.info("Ignoring duplicate webhook delivery {} for project {}", githubDeliveryId, projectId);
            return;
        }

        Build build = new Build();
        build.setProject(project);
        build.setBranch(project.getBranch());
        build.setStatus("QUEUED");
        build.setGithubDeliveryId(githubDeliveryId);
        
        if (payload.head_commit() != null) {
            build.setCommitSha(payload.head_commit().id());
            build.setCommitMessage(payload.head_commit().message());
        }

        buildRepository.save(build);
        log.info("Queued build {} for project {}", build.getId(), projectId);

        // TODO: In Phase 3, we will publish an event or call the BuildCoordinatorService here
    }
}
