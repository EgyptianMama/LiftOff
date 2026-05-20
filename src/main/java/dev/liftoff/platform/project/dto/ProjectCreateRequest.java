package dev.liftoff.platform.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProjectCreateRequest(
        @NotBlank(message = "Project name is required")
        String name,
        
        @NotBlank(message = "GitHub repository URL is required")
        @Pattern(regexp = "^https://github\\.com/.+/.+\\.git$", message = "Must be a valid GitHub HTTPS URL ending in .git")
        String githubRepoUrl,
        
        String branch,
        
        @NotBlank(message = "Subdomain is required")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain can only contain lowercase letters, numbers, and hyphens")
        String subdomain
) {
    public ProjectCreateRequest {
        if (branch == null || branch.isBlank()) {
            branch = "main";
        }
    }
}
