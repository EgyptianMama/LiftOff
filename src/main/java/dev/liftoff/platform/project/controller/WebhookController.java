package dev.liftoff.platform.project.controller;

import dev.liftoff.platform.common.exception.LiftoffException;
import dev.liftoff.platform.project.dto.GithubWebhookPayload;
import dev.liftoff.platform.project.entity.Project;
import dev.liftoff.platform.project.repository.ProjectRepository;
import dev.liftoff.platform.project.service.GithubSignatureValidator;
import dev.liftoff.platform.project.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final ProjectRepository projectRepository;
    private final GithubSignatureValidator signatureValidator;
    private final WebhookService webhookService;

    @PostMapping("/github/{projectId}")
    public ResponseEntity<Void> handleGithubWebhook(
            @PathVariable UUID projectId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody GithubWebhookPayload payload,
            HttpServletRequest request) {

        // Validate event type
        if (!"push".equals(eventType)) {
            // We only care about push events for deployments. Return 200 OK so GitHub doesn't retry.
            return ResponseEntity.ok().build();
        }

        // Fetch project to get the webhook secret
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new LiftoffException("Project not found"));

        // Validate signature
        String rawBody = extractRawBody(request);
        if (!signatureValidator.isValidSignature(rawBody, signatureHeader, project.getGithubWebhookSecret())) {
            log.warn("Invalid GitHub webhook signature for project {}", projectId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Process the payload
        webhookService.processGithubPush(projectId, payload, deliveryId);

        return ResponseEntity.ok().build();
    }

    private String extractRawBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                return new String(buf, 0, buf.length, StandardCharsets.UTF_8);
            }
        }
        log.warn("Request is not a ContentCachingRequestWrapper. Webhook signature validation will likely fail.");
        return "";
    }
}
