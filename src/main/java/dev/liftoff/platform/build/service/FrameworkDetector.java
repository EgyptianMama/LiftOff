package dev.liftoff.platform.build.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrameworkDetector {

    private final ObjectMapper objectMapper;

    public record FrameworkInfo(String framework, String buildCommand, String outputDir) {}

    public FrameworkInfo detect(Path workspaceDir) {
        File packageJsonFile = workspaceDir.resolve("package.json").toFile();
        
        if (!packageJsonFile.exists()) {
            // Fallback for static HTML sites without package.json
            log.info("No package.json found, assuming raw HTML site.");
            return new FrameworkInfo("HTML", null, ".");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(packageJsonFile);
            JsonNode dependencies = rootNode.path("dependencies");
            JsonNode devDependencies = rootNode.path("devDependencies");
            
            // Check for Next.js
            if (hasDependency(dependencies, devDependencies, "next")) {
                // Next.js requires 'output: "export"' in next.config.js for static sites,
                // but for our simple orchestrator we just run the build command.
                return new FrameworkInfo("Next.js", "npm install && npm run build", "out");
            }
            
            // Check for Vite (React/Vue/Svelte)
            if (hasDependency(dependencies, devDependencies, "vite")) {
                return new FrameworkInfo("Vite", "npm install && npm run build", "dist");
            }
            
            // Check for Create React App
            if (hasDependency(dependencies, devDependencies, "react-scripts")) {
                return new FrameworkInfo("Create React App", "npm install && npm run build", "build");
            }

            // Generic Node.js fallback
            return new FrameworkInfo("Node.js (Generic)", "npm install && npm run build", "dist");

        } catch (Exception e) {
            log.warn("Failed to parse package.json, falling back to Generic. Error: {}", e.getMessage());
            return new FrameworkInfo("Generic", "npm install && npm run build", "dist");
        }
    }

    private boolean hasDependency(JsonNode dependencies, JsonNode devDependencies, String name) {
        return (dependencies.isObject() && dependencies.has(name)) ||
               (devDependencies.isObject() && devDependencies.has(name));
    }
}
