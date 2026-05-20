package dev.liftoff.platform.build.controller;

import dev.liftoff.platform.build.service.LogStreamingService;
import dev.liftoff.platform.project.entity.Build;
import dev.liftoff.platform.project.repository.BuildRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class BuildController {

    private final BuildRepository buildRepository;
    private final LogStreamingService logStreamingService;

    @GetMapping(value = "/{buildId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamLogs(
            @PathVariable UUID buildId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {

        UUID ownerId = UUID.fromString(principal.getUsername());
        
        Build build = buildRepository.findById(buildId).orElse(null);
        if (build == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Ensure the logged in user owns the project this build belongs to
        if (!build.getProject().getOwner().getId().equals(ownerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        SseEmitter emitter = logStreamingService.subscribe(buildId);
        return ResponseEntity.ok(emitter);
    }
}
