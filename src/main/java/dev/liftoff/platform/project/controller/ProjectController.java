package dev.liftoff.platform.project.controller;

import dev.liftoff.platform.build.dto.BuildResponse;
import dev.liftoff.platform.project.dto.ProjectCreateRequest;
import dev.liftoff.platform.project.dto.ProjectResponse;
import dev.liftoff.platform.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        
        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(projectService.createProject(request, ownerId));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getUserProjects(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        
        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(projectService.getUserProjects(ownerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        
        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(projectService.getProject(id, ownerId));
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<Void> triggerDeploy(
            @PathVariable UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        
        UUID ownerId = UUID.fromString(principal.getUsername());
        projectService.triggerManualBuild(id, ownerId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/builds")
    public ResponseEntity<List<BuildResponse>> getProjectBuilds(
            @PathVariable UUID id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        
        UUID ownerId = UUID.fromString(principal.getUsername());
        return ResponseEntity.ok(projectService.getProjectBuilds(id, ownerId));
    }
}
